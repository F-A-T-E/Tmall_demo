<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8"%>
<html>
<head>
    <script>
        //初始化下拉框
        $('#product_category').selectpicker('refresh');
        //点击查询按钮时
        $("#btn_product_submit").click(function () {
            //产品名称
            var productName = $("#product_name").val();
            //分类id
            var categoryId = $("#product_category").val();
            //产品状态
            var status = $("input[type='radio'][name='lbl_product_isEnabled']:checked").val();
            if (status === undefined) {
                status = 1
            }
            //最低价
            var lowestPrice = $("#product_sale_place").val();
            //最高价
            var highestPrice = $("#product_place").val();
        });
    </script>
    <style rel="stylesheet">
        #text_cut{
            position: relative;
            right: 10px;
            color: #ccc;
        }
        #lbl_product_isEnabled_special{
            margin: 0 20px 0 0;
        }
        .bootstrap-select:not([class*=col-]):not([class*=form-control]):not(.input-group-btn){
            width: 150px;
        }
    </style>
</head>
<body>
<div class="frm_div">
    <div class="frm_group">
        <label class="frm_label" id="lbl_product_name" for="product_name">产品名称</label>
        <input class="frm_input" id="product_name" type="text" maxlength="20"/>

        <label class="frm_label" id="lbl_product_category_id" for="product_category">产品类型</label>
        <select class="selectpicker" id="product_category" data-size="10">
            <option value="0">请选择</option>
            <c:forEach items="${requestScope.categoryList}" var="category">
                <option value="${category.category_id}">${category.category_name}</option>
            </c:forEach>
    </select>
    </div>
    <div class="frm_group_last">
        <label class="frm_label" id="lbl_product_isEnabled" for="product_isEnabled_true">产品状态</label>
        <input class="frm_radio radio_isEnabled" id="product_isEnabled_true" name="lbl_product_isEnabled" type="radio" value="1" checked>
        <label class="frm_label" id="lbl_product_isEnabled_true" for="product_isEnabled_true">销售中</label>
        <input class="frm_radio radio_isEnabled" id="product_isEnabled_false" name="lbl_product_isEnabled" type="radio" value="0">
        <label class="frm_label" id="lbl_product_isEnabled_false" for="product_isEnabled_false">停售中</label>
        <input class="frm_radio radio_isEnabled" id="product_isEnabled_special" name="lbl_product_isEnabled" type="radio" value="2">
        <label class="frm_label" id="lbl_product_isEnabled_special" for="product_isEnabled_special">促销中</label>

        <label class="frm_label"  id="lbl_product_sale_place" for="product_sale_place">金额</label>
        <input class="frm_input frm_num"  id="product_sale_place" type="text" placeholder="最低价">
        <span id="text_cut">—</span>
        <input class="frm_input frm_num"  id="product_place" type="text" placeholder="最高价">

        <input class="frm_btn" id="btn_product_submit" type="button" value="查询"/>
        <input class="frm_btn frm_clear" id="btn_clear" type="button" value="重置"/>
    </div>
</div>
<div class="data_count_div">
    <svg class="icon" viewBox="0 0 1024 1024" version="1.1" xmlns="http://www.w3.org/2000/svg" p-id="2522">
            <path d="M401.976676 735.74897c-88.721671 0-172.124196-34.635845-234.843656-97.526197-62.724577-62.86784-97.271394-146.453537-97.271394-235.358379s34.546817-172.490539 97.276511-235.361449c62.715367-62.887282 146.117892-97.522104 234.838539-97.522104 88.719624 0 172.135452 34.633798 234.881518 97.522104 62.704111 62.875003 97.235578 146.4607 97.235578 235.361449 0 88.901773-34.530444 172.487469-97.231485 235.358379C574.112128 701.116195 490.6963 735.74897 401.976676 735.74897zM401.976676 121.204479c-75.012438 0-145.533584 29.290093-198.572568 82.474386-109.585861 109.834524-109.585861 288.539602-0.004093 398.36901 53.043077 53.188386 123.564223 82.47848 198.577684 82.47848 75.015507 0 145.553027-29.291117 198.620663-82.47848C710.126918 492.220514 710.126918 313.511343 600.593246 203.678866 547.530726 150.496619 476.992183 121.204479 401.976676 121.204479z" p-id="2523" fill="#FF7874">
            </path>
            <path d="M932.538427 958.228017c-6.565533 0-13.129019-2.508123-18.132986-7.52437L606.670661 642.206504c-9.989515-10.014074-9.969049-26.231431 0.045025-36.220946s26.230408-9.969049 36.220946 0.045025l307.73478 308.497143c9.989515 10.014074 9.969049 26.231431-0.045025 36.220946C945.627537 955.735244 939.081447 958.228017 932.538427 958.228017z" p-id="2524">
            </path>
        </svg>
    <span class="data_count_title">查看合计</span>
    <span>产品总数:</span>
    <span class="data_count_value">${requestScope.productCount}</span>
    <span class="data_count_unit">件</span>
</div>
<table class="table_normal" id="table_product_list">
    <thead>
    <tr>
        <th>产品名称</th>
        <th>产品标题</th>
        <th>原价</th>
        <th>促销价</th>
        <th>创建时间</th>
        <th>上架状态</th>
        <th>产品详情</th>
        <th hidden>产品ID</th>
    </tr>
    </thead>
    <tbody>
    <c:forEach items="${requestScope.productList}" var="product">
        <tr>
            <td>${product.product_name}</td>
            <td>${product.product_title}</td>
            <td>${product.product_price}</td>
            <td>${product.product_sale_price}</td>
            <td>${product.product_create_date}</td>
            <td<c:choose>
                <c:when test="${product.product_isEnabled == '停售中'}">class="td_error"</c:when>
                <c:when test="${product.product_isEnabled == '促销中'}">class="td_wait"</c:when>
                <c:otherwise>class="td_success"</c:otherwise></c:choose>>${product.product_isEnabled}</td>
            <td class="td_special"><a href="#">详情</a></td>
            <td hidden>${product.product_id}</td>
        </tr>
    </c:forEach>
    </tbody>
</table>
</body>
</html>
