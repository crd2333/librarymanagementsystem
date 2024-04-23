<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题和搜索框 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold;">
            图书管理
            <el-input v-model="toSearch" :prefix-icon="Search"
                style=" width: 15vw;min-width: 150px; margin-left: 30px; margin-right: 30px; float: right; ;"
                clearable />
        </div>

        <!-- 查询按钮、入库按钮 -->
        <div style="display: flex; justify-content: center;">
            <el-button style="margin-left: 10px;" type="primary" @click="queryVisible = true">查询</el-button>
            <el-button style="margin-left: 10px;" type="primary" @click="storeVisible = true">入库</el-button>
            <el-button style="margin-left: 10px;" type="primary" @click="batchstoreVisible = true">批量入库</el-button>
        </div>

        <!-- 查询对话框 -->
        <el-dialog v-model="queryVisible" title="查询书籍" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别（精确）：<el-input v-model="toQuery.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名（模糊）：<el-input v-model="toQuery.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者（模糊）：<el-input v-model="toQuery.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社（模糊）：<el-input v-model="toQuery.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格区间：
                <el-input v-model="toQuery.priceMin" style="width: 5vw;" clearable />
                <span> ~ </span>
                <el-input v-model="toQuery.priceMax" style="width: 5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份区间：
                <el-input v-model="toQuery.publish_yearMin" style="width: 5vw;" clearable />
                <span> ~ </span>
                <el-input v-model="toQuery.publish_yearMax" style="width: 5vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button @click="queryVisible = false; resetQuery()">取消</el-button>
                    <el-button type="primary" @click="QueryBooks" :disabled="toQuery.category == null && toQuery.title == null && toQuery.author == null && toQuery.press == null && toQuery.priceMin == null && toQuery.priceMax == null && toQuery.publish_yearMin == null && toQuery.publish_yearMax == null">确定</el-button> <!-- 均为空时禁用确定按钮 -->
                </span>
            </template>
        </el-dialog>
        <!-- 查询结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600" :default-sort="{ prop: 'bookID', order: 'ascending' }" :table-layout="'auto'" style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="bookID" label="图书ID" sortable />
            <el-table-column prop="category" label="类别" sortable />
            <el-table-column prop="title" label="书名" sortable />
            <el-table-column prop="press" label="出版社" sortable />
            <el-table-column prop="publish_year" label="出版年份" sortable />
            <el-table-column prop="author" label="作者" sortable />
            <el-table-column prop="price" label="价格" sortable />
            <el-table-column prop="stock" label="库存" />
            <el-table-column align="center" label="操作">
                <template v-slot="scope">
                    <el-button type="primary" size="small" :icon="Edit" circle @click="changeVisible = true; toChange.orig = scope.row; toChange.category = scope.row.category; toChange.title = scope.row.title; toChange.author = scope.row.author; toChange.press = scope.row.press; toChange.price = scope.row.price; toChange.publish_year = scope.row.publish_year; toChange.stock = scope.row.stock"></el-button>
                    <el-button type="danger" size="small" :icon="Delete" circle @click="deleteVisible = true; toDelete.bookID = scope.row.bookID; toDelete.title = scope.row.title; toDelete.author = scope.row.author; toDelete.press = scope.row.press; toDelete.price = scope.row.price; toDelete.publish = scope.row.publish_year; toDelete.stock = scope.row.stock"></el-button>
                    <el-button type="primary" size="small" @click="borrowVisible = true; toBorrow.bookID = scope.row.bookID; toBorrow.title = scope.row.title; toBorrow.author = scope.row.author; toBorrow.press = scope.row.press; toBorrow.price = scope.row.price; toBorrow.publish = scope.row.publish_year; toBorrow.stock = scope.row.stock">借书</el-button>
                    <el-button type="primary" size="small" @click="returnVisible = true; toReturn.bookID = scope.row.bookID; toReturn.title = scope.row.title; toReturn.author = scope.row.author; toReturn.press = scope.row.press; toReturn.price = scope.row.price; toReturn.publish = scope.row.publish_year; toReturn.stock = scope.row.stock">还书</el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 新书入库对话框 -->
        <el-dialog v-model="storeVisible" title="新书入库" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：<el-input v-model="toStore.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：<el-input v-model="toStore.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：<el-input v-model="toStore.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：<el-input v-model="toStore.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：<el-input v-model="toStore.publish_year" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：<el-input v-model="toStore.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存：<el-input v-model="toStore.stock" style="width: 12.5vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button type="primary" @click="storeVisible = false; resetStore()">取消</el-button>
                    <el-button type="primary" @click="storeNewBook" :disabled="toStore.category === null || toStore.title === null || toStore.author === null || toStore.press === null || toStore.publish_year === null || toStore.price === null || toStore.stock === null">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 新书批量入库对话框 -->
        <el-dialog v-model="batchstoreVisible" title="新书批量入库" width="30%" align-center>
            <el-upload
                class="upload-demo"
                drag
                :http-request="BatchUpload"
                action="http://localhost:8000/book"
                show-file-list="false"
                >
                <i class="el-icon-upload"></i>
                <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
                <div class="el-upload__tip" slot="tip">上传 json 文件</div>
            </el-upload>
        </el-dialog>

        <!-- 图书更改对话框 -->
        <el-dialog v-model="changeVisible" title="更改图书信息" width="30%" align-center>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                类别：<el-input v-model="toChange.category" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                书名：<el-input v-model="toChange.title" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                作者：<el-input v-model="toChange.author" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版社：<el-input v-model="toChange.press" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                出版年份：<el-input v-model="toChange.publish_year" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                价格：<el-input v-model="toChange.price" style="width: 12.5vw;" clearable />
            </div>
            <div style="margin-left: 2vw; font-weight: bold; font-size: 1rem; margin-top: 20px; ">
                库存：<el-input v-model="toChange.stock" style="width: 12.5vw;" clearable />
            </div>
            <template #footer>
                <span>
                    <el-button type="primary" @click="changeVisible = false">取消</el-button>
                    <el-button type="primary" :disabled="toChange.orig === null || toChange.orig.category === toChange.category && toChange.orig.title === toChange.title && toChange.orig.author === toChange.author && toChange.orig.press === toChange.press && toChange.orig.publish_year === toChange.publish_year && toChange.orig.price === toChange.price && toChange.orig.stock === toChange.stock" @click="changeBook">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 图书删除对话框 -->
        <el-dialog v-model="deleteVisible" title="删除图书" width="30%" align-center>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">图书ID</span>：{{toDelete.bookID}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">书名</span>：{{toDelete.title}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">作者</span>：{{toDelete.author}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版社</span>：{{toDelete.press}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">价格</span>：{{toDelete.price}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版年份</span>：{{toDelete.publish}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">库存</span>：{{toDelete.stock}}
            </div>
            <template #footer>
                <span>
                    <el-button type="primary" @click="deleteVisible = false">取消</el-button>
                    <el-button type="primary" @click="deleteBook">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 借书对话框 -->
        <el-dialog v-model="borrowVisible" title="新建借书记录" width="30%" align-center>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">图书ID</span>：{{toBorrow.bookID}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">书名</span>：{{toBorrow.title}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">作者</span>：{{toBorrow.author}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版社</span>：{{toBorrow.press}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">价格</span>：{{toBorrow.price}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版年份</span>：{{toBorrow.publish}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">库存</span>：{{toBorrow.stock}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">借书证ID</span>：
                <el-input v-model="toBorrow.cardID" style="width: 12.5vw;" placeholder="输入借书证ID" clearable />
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">借出时间</span>：now( )
            </div>
            <template #footer>
                <span>
                    <el-button type="primary" @click="borrowVisible = false; toBorrow.cardID = null">取消</el-button>
                    <el-button type="primary" @click="borrowBook" :disabled="toBorrow.cardID === null">确定</el-button>
                </span>
            </template>
        </el-dialog>

        <!-- 还书对话框 -->
        <el-dialog v-model="returnVisible" title="还书" width="30%" align-center>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">图书ID</span>：{{toReturn.bookID}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">书名</span>：{{toReturn.title}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">作者</span>：{{toReturn.author}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版社</span>：{{toReturn.press}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">价格</span>：{{toReturn.price}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">出版年份</span>：{{toReturn.publish}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">库存</span>：{{toReturn.stock}}
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">借书证ID</span>：
                <el-input v-model="toReturn.cardID" style="width: 12.5vw;" placeholder="输入借书证ID" clearable />
            </div>
            <div style="margin-left: 2vw; font-size: 1rem; ">
                <span style="font-weight: bold;">还书时间</span>：now( )
            </div>
            <template #footer>
                <span>
                    <el-button type="primary" @click="returnVisible = false; toReturn.cardID = null">取消</el-button>
                    <el-button type="primary" @click="returnBook" :disabled="toReturn.cardID === null">确定</el-button>
                </span>
            </template>
        </el-dialog>

    </el-scrollbar>
</template>

<script>
import { Delete, Edit, Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
    data() {
        return {
            isShow: false, // 结果表格展示状态
            tableData: [{
                bookID: 1,
                category: "test-category",
                title: "test-title",
                press: "test-press",
                author: "test-author",
                publish_year: 0,
                price: 0,
                stock: 0,
            }],
            toQuery: { // 待查询内容(对某些信息进行查询)
                category: null,
                title: null,
                author: null,
                press: null,
                priceMin: null,
                priceMax: null,
                publish_yearMin: null,
                publish_yearMax: null,
            },
            toChange: { // 待更改内容
                orig: null,
                category: null,
                title: null,
                author: null,
                press: null,
                publish_year: null,
                price: null,
                stock: null,
            },
            toDelete: { // 待删除内容
                bookID: null,
                category: null,
                title: null,
                author: null,
                press: null,
                publish_year: null,
                price: null,
                stock: null,
            },
            toBorrow: { // 待借书内容
                bookID: null,
                category: null,
                title: null,
                author: null,
                press: null,
                price: null,
                publish_year: null,
                cardID: null,
            },
            toReturn: { // 待还书内容
                bookID: null,
                category: null,
                title: null,
                author: null,
                press: null,
                price: null,
                publish: null,
                cardID: null,
            },
            toStore: { // 待入库内容
                category: null,
                title: null,
                author: null,
                press: null,
                publish_year: null,
                price: null,
                stock: null,
            },
            toSearch: '', // 待搜索内容(对查询到的结果进行搜索)
            queryVisible: false, // 查询对话框可见性
            storeVisible: false, // 新书入库对话框可见性
            batchstoreVisible: false, // 批量入库对话框可见性
            deleteVisible: false, // 删除对话框可见性
            changeVisible: false, // 图书更改对话框可见性
            borrowVisible: false, // 借书对话框可见性
            returnVisible: false, // 还书对话框可见性
            Delete,
            Edit,
            Search,
        }
    },
    computed: {
        fitlerTableData() { // 搜索规则
            return this.tableData.filter(
                (tuple) =>
                    (this.toSearch == '') || // 搜索框为空，即不搜索，全部显示
                    tuple.bookID == this.toSearch || // 图书号与搜索要求一致
                    tuple.category.includes(this.toSearch) || // 类别包含搜索要求
                    tuple.title.includes(this.toSearch) || // 书名包含搜索要求
                    tuple.author.includes(this.toSearch) || // 作者包含搜索要求
                    tuple.press.includes(this.toSearch) || // 出版社包含搜索要求
                    tuple.price.toString() == this.toSearch || // 价格与搜索要求一致
                    tuple.publish_year.toString() == this.toSearch // 出版年份与搜索要求一致
            )
        }
    },
    methods: {
        QueryBooks() {
            this.tableData = [] // 清空列表
            axios.get('/book', // 向 /book 发出 GET 请求
                {params: {
                    category: this.toQuery.category,
                    title: this.toQuery.title,
                    author: this.toQuery.author,
                    press: this.toQuery.press,
                    priceMin: this.toQuery.priceMin,
                    priceMax: this.toQuery.priceMax,
                    publish_yearMin: this.toQuery.publish_yearMin,
                    publish_yearMax: this.toQuery.publish_yearMax
                }})
                .then(response => {
                    let books = response.data // 获取响应负载
                    books.forEach(book => {
                        this.tableData.push(book)
                    });
                    ElMessage.success("查询成功") // 显示消息提醒，这好像不太会失败，就直接把数据放 data 里
                    this.queryVisible = false // 关闭查询对话框
                    this.isShow = true // 显示结果列表
                })
        },
        resetQuery() {
            this.toQuery = {
                category: null,
                title: null,
                author: null,
                press: null,
                priceMin: null,
                priceMax: null,
                publish_yearMin: null,
                publish_yearMax: null,
            }
        },
        resetStore() {
            this.toStore = {
                category: null,
                title: null,
                author: null,
                press: null,
                publish_year: null,
                price: null,
                stock: null,
            }
        },
        storeNewBook() {
            axios.post("/book", // 向 /book 发出 POST 请求
                { // 请求体
                    category: this.toStore.category,
                    title: this.toStore.title,
                    author: this.toStore.author,
                    press: this.toStore.press,
                    publish_year: this.toStore.publish_year,
                    price: this.toStore.price,
                    stock: this.toStore.stock
                })
                .then(response => {
                    if (response.data == "Store book successfully!")
                        ElMessage.success("入库成功")
                    else
                        ElMessage.error("入库失败")
                    this.storeVisible = false // 关闭入库对话框
                    this.resetStore() // 重置入库条件
                    this.QueryBooks() // 重新查询刷新页面
                })
        },
        changeBook() {
            axios.put("/book", // 向 /book 发出 PUT 请求
                { // 请求体
                    bookID: this.toChange.orig.bookID,
                    category: this.toChange.category,
                    title: this.toChange.title,
                    author: this.toChange.author,
                    press: this.toChange.press,
                    publish_year: this.toChange.publish_year,
                    price: this.toChange.price,
                    stock: this.toChange.stock - this.toChange.orig.stock
                })
                .then(response => {
                    if (response.data == "Modify book successfully!")
                        ElMessage.success("更改成功")
                    else if (response.data == "One of the input is null.")
                        ElMessage.error("输入条件不可为空")
                    else
                        ElMessage.error("更改失败")
                    this.changeVisible = false // 关闭更改对话框
                    this.toChange = { // 重置更改条件
                        orig: null,
                        category: null,
                        title: null,
                        author: null,
                        press: null,
                        publish_year: null,
                        price: null,
                        stock: null,
                    }
                    this.QueryBooks() // 重新查询刷新页面
                })
        },
        deleteBook() {
            axios.delete("/book?id=" + this.toDelete.bookID) // 向 /book 发出 DELETE 请求
            .then(response => {
                if (response.data == "Remove book successfully!")
                    ElMessage.success("删除成功")
                else
                    ElMessage.error("删除失败")
                this.deleteVisible = false // 关闭删除对话框
                this.toDelete = { // 重置删除条件
                    bookID: null,
                    category: null,
                    title: null,
                    author: null,
                    press: null,
                    publish_year: null,
                    price: null,
                    stock: null,
                }
                this.QueryBooks() // 重新查询刷新页面
            })
        },
        borrowBook() {
            axios.patch("/book",
                { // 请求体
                    command_type: "borrow",
                    bookID: this.toBorrow.bookID,
                    cardID: this.toBorrow.cardID,
                    // borrowTime created in backend
                })
                .then(response => {
                    if (response.data == "Borrow book successfully!")
                        ElMessage.success("借书成功")
                    else
                        ElMessage.error("借书失败")
                    this.borrowVisible = false // 关闭借书对话框
                    this.toBorrow = { // 重置借书条件
                        bookID: null,
                        category: null,
                        title: null,
                        author: null,
                        press: null,
                        price: null,
                        publish_year: null,
                        cardID: null,
                    }
                    this.QueryBooks() // 重新查询刷新页面
                })
        },
        returnBook() {
            axios.patch("/book",
                { // 请求体
                    command_type: "return",
                    bookID: this.toReturn.bookID,
                    cardID: this.toReturn.cardID,
                    // return time created in backend
                })
                .then(response => {
                    if (response.data == "Return book successfully!")
                        ElMessage.success("还书成功")
                    else
                        ElMessage.error("还书失败")
                    this.returnVisible = false // 关闭还书对话框
                    this.toReturn = { // 重置还书条件
                        bookID: null,
                        category: null,
                        title: null,
                        author: null,
                        press: null,
                        price: null,
                        publish_year: null,
                        cardID: null,
                    }
                    this.QueryBooks() // 重新查询刷新页面
                })
        },
        BatchUpload(file){
            let formdata = new FormData()
            formdata.append('file', file.file)
            axios.post("/book?uploadfile=true", formdata)
            .then(response => {
                if (response.data == "Batch store successfully!") {
                    ElMessage.success("批量入库成功")
                    this.batchstoreVisible = false
                    this.QueryBooks()
                } else if (response.data == "The json data is invalid!") {
                    ElMessage.error("入库失败！请检查图书格式及其完整性")
                } else {
                    ElMessage.error("批量入库失败")
                }
            })
        },
    }
}
</script>