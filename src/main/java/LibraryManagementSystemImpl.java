import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.HashSet;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    // 图书入库模块。向图书库中注册(添加)一本新书，并返回新书的书号。如果该书已经存在于图书库中，那么入库操作将失败。当且仅当书的<类别, 书名, 出版社, 年份, 作者>均相同时，才认为两本书相同。请注意，book_id作为自增列，应该插入时由数据库生成。插入完成后，需要根据数据库生成的book_id值去更新book对象里的book_id。
    @Override
    public ApiResult storeBook(Book book) {
        Connection conn = connector.getConn();
        try {
            String sql1 = "select book_id from book where category = ? and title = ? and press = ? and publish_year = ? and author = ?";
            String sql2 = "insert into book (category, title, press, publish_year, author, price, stock) values (?, ?, ?, ?, ?, ?, ?)";
            ResultSet rset = null;
            PreparedStatement pstmt = conn.prepareStatement(sql1); // check if exist
            pstmt.setString(1, book.getCategory());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getPress());
            pstmt.setInt(4, book.getPublishYear());
            pstmt.setString(5, book.getAuthor());
            rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    throw new Exception("The book already exist!");
            }
            pstmt = conn.prepareStatement(sql2); // insert book, use prepared_statement to avoid SQL injection
            pstmt.setString(1, book.getCategory());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getPress());
            pstmt.setInt(4, book.getPublishYear());
            pstmt.setString(5, book.getAuthor());
            pstmt.setDouble(6, book.getPrice());
            pstmt.setInt(7, book.getStock());
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(sql1); // get book_id from inserted book
            pstmt.setString(1, book.getCategory());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getPress());
            pstmt.setInt(4, book.getPublishYear());
            pstmt.setString(5, book.getAuthor());
            rset = pstmt.executeQuery();
            while (rset.next()) { // update object's book_id according to the inserted book
                if (!rset.wasNull())
                    book.setBookId(rset.getInt("book_id"));
            }
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Store book successfully!");
    }

    // 图书增加库存模块。为图书库中的某一本书增加库存。其中库存增量deltaStock可正可负，若为负数，则需要保证最终库存是一个非负数。
    @Override
    public ApiResult incBookStock(int bookId, int deltaStock) {
        Connection conn = connector.getConn();
        int newstock = -1;
        try {
            String sql1 = "select stock from book where book_id = ?";
            String sql2 = "update book set stock = ? where book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1); // get stock, use prepared_stamement to avoid SQL injection
            pstmt.setInt(1, bookId);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) { // check if the book exist
                newstock = rset.getInt("stock");
                if (!rset.wasNull())
                    break;
            }
            if (newstock == -1)
                throw new Exception("The book does not exist!");
            newstock = newstock + deltaStock;
            if (newstock < 0) // check if newstock is negative
                throw new Exception("Stock can't be negative!");
            pstmt = conn.prepareStatement(sql2); // update the book stock
            pstmt.setInt(1, newstock);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Modify book stock successfully!");
    }

    // 图书批量入库模块。批量入库图书，如果有一本书入库失败，那么就需要回滚整个事务(即所有的书都不能被入库)。
    @Override
    public ApiResult storeBook(List<Book> books) {
        Connection conn = connector.getConn();
        try { // use batch to store books
            String sql1 = "select category, title, press, publish_year, author from book";
            String sql2 = "insert into book (category, title, press, publish_year, author, price, stock) values (?, ?, ?, ?, ?, ?, ?)";
            String sql3 = "select book_id from book where category = ? and  title = ? and press = ? and publish_year = ? and author = ?";
            PreparedStatement pstmt = null;
            ResultSet rset = null;
            Set<Book> bookSet = new HashSet<>(books);
            Integer num = bookSet.size();
            if (num < books.size())
                throw new Exception("There are two same books in the list!");
            pstmt = conn.prepareStatement(sql1); // check if one of the books already exist
            rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    bookSet.add(new Book(rset.getString("category"), rset.getString("title"), rset.getString("press"), rset.getInt("publish_year"), rset.getString("author"), 0.0, 0));
                    num++;
                }
            }
            if (bookSet.size() != num)
                throw new Exception("One of the books already exist!");
            pstmt = conn.prepareStatement(sql2); // use batch to insert books
            for (Book tmp_book : books) {
                pstmt.setString(1, tmp_book.getCategory());
                pstmt.setString(2, tmp_book.getTitle());
                pstmt.setString(3, tmp_book.getPress());
                pstmt.setInt(4, tmp_book.getPublishYear());
                pstmt.setString(5, tmp_book.getAuthor());
                pstmt.setDouble(6, tmp_book.getPrice());
                pstmt.setInt(7, tmp_book.getStock());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            pstmt = conn.prepareStatement(sql3);
            for (Book tmp_book : books) { // update object's book_id according to the inserted book
                pstmt.setString(1, tmp_book.getCategory());
                pstmt.setString(2, tmp_book.getTitle());
                pstmt.setString(3, tmp_book.getPress());
                pstmt.setInt(4, tmp_book.getPublishYear());
                pstmt.setString(5, tmp_book.getAuthor());
                rset = pstmt.executeQuery();
                while (rset.next()) {
                    if (!rset.wasNull())
                        tmp_book.setBookId(rset.getInt("book_id"));
                }
            }
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Batch store successfully!");
    }

    // 图书删除模块。从图书库中删除一本书。如果还有人尚未归还这本书，那么删除操作将失败。
    @Override
    public ApiResult removeBook(int bookId) {
        Connection conn = connector.getConn();
        boolean isExist = false;
        try {
            String sql1 = "select * from book where book_id = ?";
            String sql2 = "select * from borrow where book_id = ? and return_time = 0";
            String sql3 = "delete from book where book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1); // check if the book exist, use prepared_stamement to avoid SQL injection
            pstmt.setInt(1, bookId);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist)
                throw new Exception("The book does not exist!");
            pstmt = conn.prepareStatement(sql2); // check if the book is borrowed
            pstmt.setInt(1, bookId);
            rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    throw new Exception("The book is borrowed!");
            }
            pstmt = conn.prepareStatement(sql3); // delete the book
            pstmt.setInt(1, bookId);
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Remove book successfully!");
    }

    // 图书修改模块。修改已入库图书的基本信息，该接口不能修改图书的书号和存量。
    @Override
    public ApiResult modifyBookInfo(Book book) {
        Connection conn = connector.getConn();
        try {
            String sql = "update book set category = ?, title = ?, press = ?, publish_year = ?, author = ?, price = ? where book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, book.getCategory());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getPress());
            pstmt.setInt(4, book.getPublishYear());
            pstmt.setString(5, book.getAuthor());
            pstmt.setDouble(6, book.getPrice());
            pstmt.setInt(7, book.getBookId());
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Modify book successfully!");
    }

    // 图书查询模块。根据提供的查询条件查询符合条件的图书，并按照指定排序方式排序。查询条件包括：类别点查(精确查询)，书名点查(模糊查询)，出版社点查(模糊查询)，年份范围查，作者点查(模糊查询)，价格范围差。如果两条记录排序条件的值相等，则按book_id升序排序。
    @Override
    public ApiResult queryBook(BookQueryConditions conditions) {
        Connection conn = connector.getConn();
        List<Book> books = new ArrayList<>();
        try {
            String sql = "select * from book where " +
                "title like ? and press like ? and publish_year >= ? " +
                "and publish_year <= ? and author like ? and price >= ? and price <= ?";
            if (conditions.getCategory() != null) // check for if need to add category condition(precise query)
                sql = sql + " and category = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            if (conditions.getCategory() != null) // if category condition exist, set it
                pstmt.setString(8, conditions.getCategory());
            if (conditions.getTitle() != null)
                pstmt.setString(1, "%" + conditions.getTitle() + "%");
            else
                pstmt.setString(1, "%");
            if (conditions.getPress() != null)
                pstmt.setString(2, "%" + conditions.getPress() + "%");
            else
                pstmt.setString(2, "%");
            if (conditions.getMinPublishYear() != null)
                pstmt.setInt(3, conditions.getMinPublishYear());
            else
                pstmt.setInt(3, -99999);
            if (conditions.getMaxPublishYear() != null)
                pstmt.setInt(4, conditions.getMaxPublishYear());
            else
                pstmt.setInt(4, 99999);
            if (conditions.getAuthor() != null)
                pstmt.setString(5, "%" + conditions.getAuthor() + "%");
            else
                pstmt.setString(5, "%");
            if (conditions.getMinPrice() != null)
                pstmt.setDouble(6, conditions.getMinPrice());
            else
                pstmt.setDouble(6, -99999.9);
            if (conditions.getMaxPrice() != null)
                pstmt.setDouble(7, conditions.getMaxPrice());
            else
                pstmt.setDouble(7, 99999.9);
            ResultSet rset = pstmt.executeQuery();
            Book tmp_Book = null;
            while (rset.next()) {
                if (!rset.wasNull()) {
                    tmp_Book = new Book(rset.getString("category"), rset.getString("title"), rset.getString("press"), rset.getInt("publish_year"), rset.getString("author"), rset.getDouble("price"), rset.getInt("stock"));
                    tmp_Book.setBookId(rset.getInt("book_id"));
                    books.add(tmp_Book);
                }
            }
            Comparator<Book> comparator = conditions.getSortBy().getComparator(); // order first by conditions.getSortBy().getComparator()
            if (conditions.getSortOrder() == SortOrder.ASC)
                comparator = comparator.thenComparing(book -> book.getBookId()); // then by book_id
            else
                comparator = comparator.reversed().thenComparing(book -> book.getBookId()); // if necessary, reverse and then by book_id
            books.sort(comparator);
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage(), null);
        }
        return new ApiResult(true, "Query book successfully!", new BookQueryResults(books));
    }

    // 借书模块。根据给定的书号、卡号和借书时间添加一条借书记录，然后更新库存。若用户此前已经借过这本书但尚未归还，那么借书操作将失败。
    @Override
    public ApiResult borrowBook(Borrow borrow) {
        Connection conn = connector.getConn();
        try {
            String sql1 = "select * from book natural join borrow where book_id = ? and (stock = 0 or card_id = ? and return_time = 0) LOCK IN SHARE MODE";
            String sql2 = "insert into borrow (book_id, card_id, borrow_time, return_time) values (?, ?, ?, 0)";
            String sql3 = "update book set stock = stock - 1 where book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1); // check if the book is borrowed or out of stock
            pstmt.setInt(1, borrow.getBookId());
            pstmt.setInt(2, borrow.getCardId());
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    throw new Exception("You have borrowed the book or the book is out of stock!");
            }
            pstmt = conn.prepareStatement(sql2); // insert borrow record
            pstmt.setInt(1, borrow.getBookId());
            pstmt.setInt(2, borrow.getCardId());
            pstmt.setLong(3, borrow.getBorrowTime());
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(sql3); // update book stock
            pstmt.setInt(1, borrow.getBookId());
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Borrow book successfully!");
    }

    // 还书模块。根据给定的书号、卡号和还书时间，查询对应的借书记录，并补充归还时间，然后更新库存。
    @Override
    public ApiResult returnBook(Borrow borrow) {
        Connection conn = connector.getConn();
        boolean isExist = false;
        try {
            String sql1 = "select * from borrow where book_id = ? and card_id = ? and return_time = 0";
            String sql2 = "update borrow set return_time = ? where book_id = ? and card_id = ? and return_time = 0";
            String sql3 = "update book set stock = stock + 1 where book_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1);
            pstmt.setInt(1, borrow.getBookId());
            pstmt.setInt(2, borrow.getCardId());
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist)
                throw new Exception("The book is not borrowed or has been returned!");
            else if (rset.getLong("borrow_time") >= borrow.getReturnTime())
                throw new Exception("The return time is earlier than borrow time!");
            pstmt = conn.prepareStatement(sql2);
            pstmt.setLong(1, borrow.getReturnTime());
            pstmt.setInt(2, borrow.getBookId());
            pstmt.setInt(3, borrow.getCardId());
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(sql3);
            pstmt.setInt(1, borrow.getBookId());
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Return book successfully!");
    }

    // 借书记录查询模块。查询某个用户的借书记录，按照借书时间递减、书号递增的方式排序。
    @Override
    public ApiResult showBorrowHistory(int cardId) {
        Connection conn = connector.getConn();
        List<BorrowHistories.Item> borrows = new ArrayList<>(); // Item's constructor contains int, book and borrow instance
        try {
            String sql = "select * from borrow natural join book where card_id = ? order by borrow_time desc, book_id asc";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, cardId);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    Book book_record = new Book(rset.getString("category"), rset.getString("title"), rset.getString("press"), rset.getInt("publish_year"), rset.getString("author"), rset.getDouble("price"), rset.getInt("stock"));
                    book_record.setBookId(rset.getInt("book_id"));
                    Borrow borrow_record = new Borrow(rset.getInt("book_id"), rset.getInt("card_id"));
                    borrow_record.setBorrowTime(rset.getLong("borrow_time"));
                    borrow_record.setReturnTime(rset.getLong("return_time"));
                    borrows.add(new BorrowHistories.Item(cardId, book_record, borrow_record));
                }
            }
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage(), null);
        }
        return new ApiResult(true, "Query borrow history successfully!", new BorrowHistories(borrows));
    }

    // 借书证注册模块。注册一个借书证，若借书证已经存在，则该操作将失败。当且仅当<姓名, 单位, 身份>均相同时，才认为两张借书证相同。
    @Override
    public ApiResult registerCard(Card card) {
        Connection conn = connector.getConn();
        try {
            String sql1 = "select card_id from card where name = ? and department = ? and type = ?";
            String sql2 = "insert into card (name, department, type) values (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql1); // check if the card already exist
            pstmt.setString(1, card.getName());
            pstmt.setString(2, card.getDepartment());
            pstmt.setString(3, card.getType().getStr()); // type is a enam, use getStr()
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    throw new Exception("The card already exist!");
            }
            pstmt = conn.prepareStatement(sql2); // insert card
            pstmt.setString(1, card.getName());
            pstmt.setString(2, card.getDepartment());
            pstmt.setString(3, card.getType().getStr());
            pstmt.executeUpdate();
            pstmt = conn.prepareStatement(sql1); // update card_id according to the inserted card
            pstmt.setString(1, card.getName());
            pstmt.setString(2, card.getDepartment());
            pstmt.setString(3, card.getType().getStr());
            rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    card.setCardId(rset.getInt("card_id"));
            }
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Register card successfully!");
    }

    // 删除借书证模块。如果该借书证还有未归还的图书，那么删除操作将失败。
    @Override
    public ApiResult removeCard(int cardId) {
        Connection conn = connector.getConn();
        boolean isExist = false;
        try {
            String sql1 = "select * from card where card_id = ?";
            String sql2 = "select * from borrow where card_id = ? and return_time = 0";
            String sql3 = "delete from card where card_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1); // check if the card exist
            pstmt.setInt(1, cardId);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist)
                throw new Exception("The card does not exist!");
            pstmt = conn.prepareStatement(sql2); // check if the card has unreturned books
            pstmt.setInt(1, cardId);
            rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull())
                    throw new Exception("The card has borrowed books!");
            }
            pstmt = conn.prepareStatement(sql3); // delete the card
            pstmt.setInt(1, cardId);
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Remove card successfully!");
    }

    // 借书证查询模块。列出所有的借书证。
    @Override
    public ApiResult showCards() {
        Connection conn = connector.getConn();
        List<Card> cards = new ArrayList<>();
        try {
            String sql = "select * from card order by card_id";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    Card card_record = new Card(rset.getInt("card_id"), rset.getString("name"), rset.getString("department"), Card.CardType.values(rset.getString("type")));
                    cards.add(card_record);
                }
            }
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage(), null);
        }
        return new ApiResult(true, "Show cards successfully!", new CardList(cards));
    }

    // 自己加的借书证修改模块，方便前端使用
    @Override
    public ApiResult modifyCard(int cardId, String name, String department, Card.CardType type) {
        Connection conn = connector.getConn();
        boolean isExist = false;
        try {
            String sql1 = "select * from card where card_id = ?";
            String sql2 = "update card set name = ?, department = ?, type = ? where card_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql1);
            pstmt.setInt(1, cardId);
            ResultSet rset = pstmt.executeQuery();
            while (rset.next()) {
                if (!rset.wasNull()) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist)
                throw new Exception("The card does not exist!");
            pstmt = conn.prepareStatement(sql2);
            pstmt.setString(1, name);
            pstmt.setString(2, department);
            pstmt.setString(3, type.getStr());
            pstmt.setInt(4, cardId);
            pstmt.executeUpdate();
            pstmt.close();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, "Modify card successfully!");
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
