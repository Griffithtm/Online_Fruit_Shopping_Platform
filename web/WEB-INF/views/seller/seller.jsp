<%@ page pageEncoding="UTF-8" %>
    <%@ include file="/WEB-INF/views/header.jsp" %>
        <h1>Store management</h1>
        <p>Listings require Admin approval before publication. Seller accounts do not require approval.</p>
        <div class="two-column">
            <section class="panel">
                <h2>Create / edit a listing</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="listing">
                    <label>Listing ID (selected automatically)
                        <input class="form-control" name="id" type="number" readonly value="${editListing.id}">
                    </label>
                    <label>Fruit name
                        <input class="form-control" name="title" maxlength="200" autocomplete="off" data-suggestions="${pageContext.request.contextPath}/seller/suggestions" data-product-url="${pageContext.request.contextPath}/product" type="text" value="${fn:escapeXml(editListing.title)}" required>
                    </label>
                    <label>Description
                        <input class="form-control" name="description" type="text" value="${fn:escapeXml(editListing.description)}" required>
                    </label>
                    <label>Category
                        <select class="form-select" name="category" required>
                            <c:forEach items="${categories}" var="x">
                                <option value="${x.id}" ${editListing.category_id == x.id ? 'selected' : ''}>
                                    <c:out value="${x.name}" /> (#${x.id})</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>Origin
                        <input class="form-control" name="origin" type="text" value="${fn:escapeXml(editListing.origin)}" required>
                    </label>
                    <label>Harvest information
                        <input class="form-control" name="harvest" type="text" value="${fn:escapeXml(editListing.harvest_info)}">
                    </label>
                    <button class="button">Save draft</button> <a href="${pageContext.request.contextPath}/seller">New listing</a>
                </form>
            </section>
            <section class="panel">
                <h2>Create / edit variants and prices</h2>
                <form method="post" action="">
                    <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                    <input type="hidden" name="action" value="variant">
                    <label>Variant ID (selected automatically)
                        <input class="form-control" name="id" type="number" readonly value="${editVariant.id}">
                    </label>
                    <label>Listing
                        <select class="form-select" name="listing" required>
                            <c:forEach items="${listings}" var="x">
                                <option value="${x.id}" ${editVariant.listing_id == x.id ? 'selected' : ''}>
                                    <c:out value="${x.title}" /> (#${x.id})</option>
                            </c:forEach>
                        </select>
                    </label>
                    <label>Pack size (1 kg bag, 500 g box...)
                        <input class="form-control" name="option" type="text" value="${fn:escapeXml(editVariant.option_name)}" required>
                    </label>
                    <label>Selling unit
                        <input class="form-control" name="unit" type="text" value="${fn:escapeXml(editVariant.unit)}" required>
                    </label>
                    <label>Selling price (VND)
                        <input class="form-control" name="price" type="number" value="${fn:escapeXml(editVariant.price)}" required>
                    </label>
                    <button class="button">Save variant</button> <a href="${pageContext.request.contextPath}/seller">New variant</a>
                </form>
            </section>
        </div>
        <h2>Listing</h2>
        <div class="table-responsive">
            <table class="table">
                <tr>
                    <th>ID / Name</th>
                    <th>Status</th>
                    <th>Reason</th>
                    <th>Actions</th>
                </tr>
                <c:forEach items="${listings}" var="l">
                    <tr>
                        <td>#${l.id}
                            <c:out value="${l.title}" />
                        </td>
                        <td>${l.status}</td>
                        <td>
                            <c:out value="${l.rejection_reason}" />
                        </td>
                        <td>
                            <form method="post" action="">
                                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                                <input type="hidden" name="action" value="submit">
                                <input type="hidden" name="id" value="${l.id}">
                                <button class="button">Submit for review</button>
                            </form><a href="?editListing=${l.id}">Edit listing</a> · <a href="?images=${l.id}">Reorder images</a>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </div>
        <section class="panel">
            <h2>Listing images</h2>
            <form method="post" enctype="multipart/form-data" action="">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <input type="hidden" name="action" value="upload">
                <label>Listing
                    <select class="form-select" name="id" required>
                        <c:forEach items="${listings}" var="x">
                            <option value="${x.id}">
                                <c:out value="${x.title}" /> (#${x.id})</option>
                        </c:forEach>
                    </select>
                </label>
                <label>Choose up to 4 images (5 MB each). This replaces the current images.
                    <input class="form-control" type="file" name="photos" accept="image/png,image/jpeg" multiple required>
                </label>
                <button class="button">Upload images</button>
            </form>
            <c:forEach items="${images}" var="im">
                <div class="cartline"><img width="90" src="${pageContext.request.contextPath}${fn:escapeXml(im.path)}" alt="Listing image">
                    <form method="post" action="">
                        <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                        <input type="hidden" name="action" value="reorder">
                        <input type="hidden" name="id" value="${fn:escapeXml(param.images)}">
                        <input type="hidden" name="position" value="${im.position}">
                        <button class="button">Move up</button>
                    </form>
                </div>
            </c:forEach>
        </section>
        <h2>Variant</h2>
        <table class="table">
            <tr>
                <th>ID / Product</th>
                <th>Pack size</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Status</th>
                <th></th>
            </tr>
            <c:forEach items="${variants}" var="v">
                <tr>
                    <td>#${v.id}
                        <c:out value="${v.title}" />
                    </td>
                    <td>
                        <c:out value="${v.option_name}" />
                    </td>
                    <td>
                        <fmt:formatNumber value="${v.price}" />
                    </td>
                    <td>${v.available_qty}</td>
                    <td>${v.status}</td>
                    <td>
                        <form method="post" action="">
                            <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                            <input type="hidden" name="action" value="retire">
                            <input type="hidden" name="id" value="${v.id}">
                            <input type="hidden" name="listing" value="${v.listing_id}">
                            <button class="button">Retire</button> <a href="?editVariant=${v.id}">Edit variant</a>
                        </form>
                    </td>
                </tr>
            </c:forEach>
        </table>
        <section class="panel">
            <h2>Receive stock by batch</h2>
            <form method="post" action="">
                <input type="hidden" name="csrf" value="${sessionScope.csrf}">
                <input type="hidden" name="action" value="stock">
                <label>Variant
                    <select class="form-select" name="variant" required>
                        <c:forEach items="${variants}" var="x">
                            <option value="${x.id}">
                                <c:out value="${x.option_name}" /> (#${x.id})</option>
                        </c:forEach>
                    </select>
                </label>
                <label>Batch number
                    <input class="form-control" name="batch" type="text" value="" required>
                </label>
                <label>Origin
                    <input class="form-control" name="origin" type="text" value="" required>
                </label>
                <label>Harvest date
                    <input class="form-control" name="harvest" type="date" value="" required>
                </label>
                <label>Receipt date
                    <input class="form-control" name="receipt" type="date" value="" required>
                </label>
                <label>Expiry date
                    <input class="form-control" name="expiry" type="date" value="" required>
                </label>
                <label>Supplier
                    <input class="form-control" name="supplier" type="text" value="" required>
                </label>
                <label>Quantity in selling units
                    <input class="form-control" name="quantity" type="number" value="" required>
                </label>
                <label>Unit cost (VND)
                    <input class="form-control" name="cost" type="number" value="" required>
                </label>
                <button class="button">Receive stock</button>
            </form>
        </section>
        <h2>Stock receipt history</h2>
        <table class="table">
            <tr>
                <th>Batch</th>
                <th>Product</th>
                <th>Expiry date</th>
                <th>Received / Remaining / Reserved</th>
                <th>Unit cost</th>
                <th>Total cost</th>
            </tr>
            <c:forEach items="${batches}" var="b">
                <tr>
                    <td>
                        <c:out value="${b.batch_no}" />
                    </td>
                    <td>
                        <c:out value="${b.title}" />
                    </td>
                    <td>${b.expiry_date}</td>
                    <td>${b.received_qty} / ${b.remaining_qty} / ${b.reserved_qty}</td>
                    <td>
                        <fmt:formatNumber value="${b.unit_cost}" />
                    </td>
                    <td>
                        <fmt:formatNumber value="${b.total_cost}" />
                    </td>
                </tr>
            </c:forEach>
        </table>
        <section class="panel">
            <h2>Browse marketplace suggestions</h2>
            <form method="get">
                <label>Enter a product name
                    <input class="form-control" name="q" data-suggestions="${pageContext.request.contextPath}/seller/suggestions" data-product-url="${pageContext.request.contextPath}/product" type="search" value="${fn:escapeXml(param.q)}">
                </label>
                <button class="button">Find suggestions by descending price</button>
            </form>
            <c:set var="products" value="${suggestions}" />
            <%@ include file="/WEB-INF/views/cards.jsp" %>
        </section>
        <script src="${pageContext.request.contextPath}/assets/js/seller-suggestions.js?v=2" defer></script>
<%@ include file="/WEB-INF/views/footer.jsp" %>
