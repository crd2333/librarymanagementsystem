import java.util.*;
import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Headers;
import org.json.JSONArray;
import org.json.JSONObject;

class CardHandler implements HttpHandler {
    private LibraryManagementSystem library;
    public CardHandler(LibraryManagementSystem library) {
        this.library = library;
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        try {
            // 响应头，因为是 JSON 通信
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            // 状态码为 200，也就是 status ok
            exchange.sendResponseHeaders(200, 0);
            // 获取输出流，java用流对象来进行io操作
            OutputStream outputStream = exchange.getResponseBody();

            // main logic
            List<entities.Card> cards = ((queries.CardList)library.showCards().payload).getCards();
            StringBuilder response = new StringBuilder("[");
            for (entities.Card card : cards) {
                response.append("{\"id\": ").append(card.getCardId())
                    .append(", \"name\": \"").append(card.getName())
                    .append("\", \"department\": \"").append(card.getDepartment())
                    .append("\", \"type\": \"").append(card.getType())
                    .append("\"},");
            }
            response.deleteCharAt(response.length() - 1); // delete the ending ","
            response.append("]");

            // 写
            outputStream.write(response.toString().getBytes());
            // 流一定要close！！！小心泄漏
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        try {
            // 读取 POST 请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个 buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // main logic
            // parse the requestbody to `name`, `department`, `type`
            String requestBodyString = requestBodyBuilder.toString().replace("{", "").replace("}", "").replace("\"", "");
            String [] parts = requestBodyString.split(",");
            String name       = parts[0].split(":").length == 1 ? "" : parts[0].split(":")[1].trim();
            String department = parts[1].split(":").length == 1 ? "" : parts[1].split(":")[1].trim();
            String type       = parts[2].split(":").length == 1 ? "" : (parts[2].split(":")[1].trim().equals("学生") ? "S" : "T");
            queries.ApiResult result = library.registerCard(new entities.Card(-1, name, department, entities.Card.CardType.values(type)));

            // 响应头
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            // 响应状态码 200
            exchange.sendResponseHeaders(200, 0);
            // 剩下三个和 GET 一样
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1); // 无法处理的实体
            e.printStackTrace();
        }
    }

    public void handlePatchRequest(HttpExchange exchange) throws IOException {
        try {
            // 读取 PATCH 请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个 buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // main logic
            // parse the requestbody to `id`, `name`, `department`, `type`
            String requestBodyString = requestBodyBuilder.toString().replace("{", "").replace("}", "").replace("\"", "");
            String [] parts = requestBodyString.split(",");
            int id            = Integer.parseInt(parts[0].split(":").length == 1 ? "" : parts[0].split(":")[1].trim());
            String name       = parts[1].split(":").length == 1 ? "" : parts[1].split(":")[1].trim();
            String department = parts[2].split(":").length == 1 ? "" : parts[2].split(":")[1].trim();
            String type       = parts[3].split(":").length == 1 ? "" : (parts[3].split(":")[1].trim().equals("学生") ? "S" : "T");
            queries.ApiResult result = library.modifyCard(id, name, department, entities.Card.CardType.values(type));

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    public void handleDeleteRequest(HttpExchange exchange) throws IOException {
        try {
            // 从 URL 读取 DELETE 请求参数
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();

            // main logic
            // parse the query to `id`
            String [] parts = query.split("&");
            int id = Integer.parseInt(parts[0].split("=").length == 1 ? "" : parts[0].split("=")[1].trim());
            queries.ApiResult result = library.removeCard(id);

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        // 状态码 204
        exchange.sendResponseHeaders(204, -1);
    }

    // 关键重写 handle 方法
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 允许所有域的请求，cors 处理
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PATCH, DELETE");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) { // 处理 GET
            handleGetRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) { // 处理 OPTIONS
            handleOptionsRequest(exchange);
        } else if (requestMethod.equals("POST")) { // 处理 POST
            handlePostRequest(exchange);
        } else if (requestMethod.equals("PATCH")) { // 处理 PATCH
            handlePatchRequest(exchange);
        } else if (requestMethod.equals("DELETE")) { // 处理 DELETE
            handleDeleteRequest(exchange);
        } else { // 其他请求返回 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
}


class BorrowHandler implements HttpHandler {
    private LibraryManagementSystem library;
    public BorrowHandler(LibraryManagementSystem library) {
        this.library = library;
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        try {
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();

            // main logic
            // parse the query to `id`
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm");
            String [] parts = query.split("&");
            int id = Integer.parseInt(parts[0].split("=").length == 1 ? "" : parts[0].split("=")[1].trim());
            List<queries.BorrowHistories.Item> histories = ((queries.BorrowHistories)library.showBorrowHistory(id).payload).getItems();
            StringBuilder response = new StringBuilder("[");
            for (queries.BorrowHistories.Item history : histories) {
                response.append("{\"cardID\": ").append(history.getCardId())
                    .append(", \"bookID\": ").append(history.getBookId())
                    .append(", \"title\": \"").append(history.getTitle())
                    .append("\", \"borrowTime\": \"").append(sdf.format(history.getBorrowTime() * 1000))
                    .append("\", \"returnTime\": \"").append(sdf.format(history.getReturnTime() * 1000))
                    .append("\"},");
            }
            response.deleteCharAt(response.length() - 1); // delete the ending ","
            response.append("]");

            OutputStream outputStream = exchange.getResponseBody();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            outputStream.write(response.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) { // 处理 GET
            handleGetRequest(exchange);
        } else { // 其他请求返回 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
}


class BookHandler implements HttpHandler {
    private LibraryManagementSystem library;
    public BookHandler(LibraryManagementSystem library) {
        this.library = library;
    }

    private void handleGetRequest(HttpExchange exchange) throws IOException {
        try {
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery().replace("+", " ");

            // main logic
            // parse the query to `category`, `title`, `author`, `press`, `priceMin`, `priceMax`, `publish_yearMin`, `publish_yearMax`
            // because null value will not appear at all after the process of browser, so first find if the query contains the label
            String category = null, title = null, author = null, press = null;
            Double priceMin = null, priceMax = null;
            Integer publish_yearMin = null, publish_yearMax = null;
            if (query.indexOf("category=") != -1)
                category = query.split("category=")[1].split("&")[0].trim();
            if (query.indexOf("title=") != -1)
                title = query.split("title=")[1].split("&")[0].trim();
            if (query.indexOf("author=") != -1)
                author = query.split("author=")[1].split("&")[0].trim();
            if (query.indexOf("press=") != -1)
                press = query.split("press=")[1].split("&")[0].trim();
            if (query.indexOf("priceMin=") != -1)
                priceMin = Double.parseDouble(query.split("priceMin=")[1].split("&")[0].trim());
            if (query.indexOf("priceMax=") != -1)
                priceMax = Double.parseDouble(query.split("priceMax=")[1].split("&")[0].trim());
            if (query.indexOf("publish_yearMin=") != -1)
                publish_yearMin = Integer.parseInt(query.split("publish_yearMin=")[1].split("&")[0].trim());
            if (query.indexOf("publish_yearMax=") != -1)
                publish_yearMax = Integer.parseInt(query.split("publish_yearMax=")[1].split("&")[0].trim());
            queries.BookQueryConditions condition = new queries.BookQueryConditions();
            condition.setCategory(category);
            condition.setTitle(title);
            condition.setAuthor(author);
            condition.setPress(press);
            condition.setMinPrice(priceMin);
            condition.setMaxPrice(priceMax);
            condition.setMinPublishYear(publish_yearMin);
            condition.setMaxPublishYear(publish_yearMax);
            List<entities.Book> books = ((queries.BookQueryResults)library.queryBook(condition).payload).getResults();
            StringBuilder response = new StringBuilder("[");
            for (entities.Book book : books) {
                response.append("{\"bookID\": ").append(book.getBookId())
                    .append(", \"category\": \"").append(book.getCategory())
                    .append("\", \"title\": \"").append(book.getTitle())
                    .append("\", \"author\": \"").append(book.getAuthor())
                    .append("\", \"press\": \"").append(book.getPress())
                    .append("\", \"price\": ").append(book.getPrice())
                    .append(", \"publish_year\": ").append(book.getPublishYear())
                    .append(", \"stock\": ").append(book.getStock())
                    .append("},");
            }
            response.deleteCharAt(response.length() - 1); // delete the ending ","
            response.append("]");

            OutputStream outputStream = exchange.getResponseBody();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            outputStream.write(response.toString().getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    private void handlePostRequest(HttpExchange exchange) throws IOException {
        try {
            InputStream requestBody = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();

            // main logic
            queries.ApiResult result = null;
            if (query != null) { // query == "uploadfile=true", if there are more command types, making this more specific
                // remove useless information, like content-type, WebKitFormBoundary etc.
                int startIndex = requestBodyBuilder.indexOf("[");
                int endIndex = requestBodyBuilder.lastIndexOf("]");
                String cleanedRequest = requestBodyBuilder.substring(startIndex, endIndex+1).trim();
                JSONArray jsonArray = new JSONArray(cleanedRequest);
                List<entities.Book> books = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject bookObject = jsonArray.getJSONObject(i);
                    String category = bookObject.optString("category");
                    String title = bookObject.optString("title");
                    String author = bookObject.optString("author");
                    String press = bookObject.optString("press");
                    int publish_year = bookObject.optInt("publish_year");
                    double price = bookObject.optDouble("price");
                    int stock = bookObject.optInt("stock");
                    if (category.isEmpty() || title.isEmpty() || author.isEmpty() || press.isEmpty() || publish_year == 0 || price == 0.0 || stock == 0) {
                        throw new IllegalArgumentException("Invalid book data");
                    }
                    books.add(new entities.Book(category, title, author, publish_year, press, price, stock));
                }
                result = library.storeBook(books);
            } else {
                // parse the requestbody to `category`, `title`, `author`, `press`, `publish_year`, `price`, `stock`
                String requestBodyString = requestBodyBuilder.toString().replace("{", "").replace("}", "").replace("\"", "");
                String [] parts = requestBodyString.split(",");
                String category = parts[0].split(":").length == 1 ? "" : parts[0].split(":")[1].trim();
                String title = parts[1].split(":").length == 1 ? "" : parts[1].split(":")[1].trim();
                String author = parts[2].split(":").length == 1 ? "" : parts[2].split(":")[1].trim();
                String press = parts[3].split(":").length == 1 ? "" : parts[3].split(":")[1].trim();
                int publish_year = Integer.parseInt(parts[4].split(":").length == 1 ? "" : parts[4].split(":")[1].trim());
                double price = Double.parseDouble(parts[5].split(":").length == 1 ? "" : parts[5].split(":")[1].trim());
                int stock = Integer.parseInt(parts[6].split(":").length == 1 ? "" : parts[6].split(":")[1].trim());
                result = library.storeBook(new entities.Book(category, title, author, publish_year, press, price, stock));
            }

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write("The json data is invalid!".getBytes());
            outputStream.close();
            e.printStackTrace();
        }
    }

    private void handlePutRequest(HttpExchange exchange) throws IOException {
        try {
            InputStream requestBody = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // main logic
            // parse the requestbody to `id`, `category`, `title`, `author`, `press`, `publish_year`, `price`, `stock`
            String requestBodyString = requestBodyBuilder.toString().replace("{", "").replace("}", "").replace("\"", "");
            String [] parts = requestBodyString.split(",");
            if (parts[1].split(":").length == 1 || parts[2].split(":").length == 1 || parts[3].split(":").length == 1 || parts[4].split(":").length == 1 || parts[5].split(":").length == 1 || parts[6].split(":").length == 1 || parts[7].split(":").length == 1) {// if one of the input is null
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, 0);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write("One of the input is null.".getBytes());
                outputStream.close();
                return;
            }
            int bookID = Integer.parseInt(parts[0].split(":")[1].trim());
            String category = parts[1].split(":")[1].trim();
            String title = parts[2].split(":")[1].trim();
            String author = parts[3].split(":")[1].trim();
            String press = parts[4].split(":")[1].trim();
            int publish_year = Integer.parseInt(parts[5].split(":")[1].trim());
            double price = Double.parseDouble(parts[6].split(":")[1].trim());
            int stock = Integer.parseInt(parts[7].split(":")[1].trim());
            entities.Book book = new entities.Book(category, title, author, publish_year, press, price, stock);
            book.setBookId(bookID);
            if (stock != 0)
                library.incBookStock(bookID, stock);
            queries.ApiResult result = library.modifyBookInfo(book);

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    public void handlePatchRequest(HttpExchange exchange) throws IOException {
        try {
            // 读取 PATCH 请求体
            InputStream requestBody = exchange.getRequestBody();
            // 用这个请求体（输入流）构造个 buffered reader
            BufferedReader reader = new BufferedReader(new InputStreamReader(requestBody));
            StringBuilder requestBodyBuilder = new StringBuilder();
            String line;
            // 没读完，一直读，拼到string builder里
            while ((line = reader.readLine()) != null) {
                requestBodyBuilder.append(line);
            }

            // main logic
            // parse the requestbody to `command_type`, `bookID`, `cardID`
            String requestBodyString = requestBodyBuilder.toString().replace("{", "").replace("}", "").replace("\"", "");
            String [] parts = requestBodyString.split(",");
            String command_type = parts[0].split(":").length == 1 ? "" : parts[0].split(":")[1].trim();
            int bookID = Integer.parseInt(parts[1].split(":").length == 1 ? "" : parts[1].split(":")[1].trim());
            int cardID = Integer.parseInt(parts[2].split(":").length == 1 ? "" : parts[2].split(":")[1].trim());
            queries.ApiResult result = null;
            if (command_type.equals("borrow")) {
                long borrowTime = new Date().getTime() / 1000;
                entities.Borrow borrow = new entities.Borrow(bookID, cardID);
                borrow.setBorrowTime(borrowTime);
                result = library.borrowBook(borrow);
            }
            else if (command_type.equals("return")) {
                long returnTime = new Date().getTime() / 1000;
                entities.Borrow borrow = new entities.Borrow(bookID, cardID);
                borrow.setReturnTime(returnTime);
                result = library.returnBook(borrow);
            }
            else
                result = new queries.ApiResult(false, "Invalid command type.");

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    private void handleDeleteRequest(HttpExchange exchange) throws IOException {
        try {
            URI requestURI = exchange.getRequestURI();
            String query = requestURI.getQuery();

            // main logic
            // parse the query to `id`
            String [] parts = query.split("&");
            int id = Integer.parseInt(parts[0].split("=").length == 1 ? "" : parts[0].split("=")[1].trim());
            queries.ApiResult result = library.removeBook(id);

            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(result.message.getBytes());
            outputStream.close();
        } catch (IOException e) {
            exchange.sendResponseHeaders(500, -1);
            e.printStackTrace();
        }
    }

    private void handleOptionsRequest(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Access-Control-Allow-Methods", "GET, PUT, POST, PATCH, DELETE");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        String requestMethod = exchange.getRequestMethod();
        if (requestMethod.equals("GET")) { // 处理 GET
            handleGetRequest(exchange);
        } else if (requestMethod.equals("POST")) { // 处理 POST
            handlePostRequest(exchange);
        } else if (requestMethod.equals("PUT")) { // 处理 PUT
            handlePutRequest(exchange);
        } else if (requestMethod.equals("PATCH")) { // 处理 PATCH
            handlePatchRequest(exchange);
        } else if (requestMethod.equals("DELETE")) { // 处理 DELETE
            handleDeleteRequest(exchange);
        } else if (requestMethod.equals("OPTIONS")) { // 处理 OPTIONS
            handleOptionsRequest(exchange);
        } else { // 其他请求返回 405 Method Not Allowed
            exchange.sendResponseHeaders(405, -1);
        }
    }
}