/* Freshora Notifications AJAX Polling
 * Header: /notifications/unread and /notifications/recent
 * Page: /notifications/list and /notifications/recent (unread view)
 * POST: /notifications/read and /notifications/read-all (form encoded + csrf)
 * Safe text insertion only: notification content never goes through innerHTML.
 */
(function () {
    'use strict';

    function start() {
        var root = document.getElementById('fr-notif-root');
        if (!root) return; // Guest: no authenticated notification UI.

        var base = root.dataset.base || '';
        var csrf = root.dataset.csrf || '';
        var unreadBadge = document.getElementById('fr-notif-count');
        var unreadLabel = document.getElementById('fr-notif-unread-label');
        var bell = document.getElementById('fr-notif-bell');
        var menu = document.getElementById('fr-notif-menu');
        var menuItems = document.getElementById('fr-notif-menu-items');
        var fullPage = document.getElementById('fr-notif-page');
        var badgeCount = 0;
        var polling = false;
        var pageState = { page: 0, filter: 'all', hasMore: false, busy: false, seq: 0 };

        function make(tag, className, text) {
            var el = document.createElement(tag);
            if (className) el.className = className;
            if (text !== undefined) el.textContent = text;
            return el;
        }

        async function getJson(path) {
            var response = await fetch(base + path, { credentials: 'same-origin', cache: 'no-store' });
            if (response.status === 401 || response.redirected) {
                throw new Error('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
            }
            if (!response.ok) throw new Error('Không thể tải thông báo. Vui lòng thử lại.');
            return response.json();
        }

        async function postForm(path, fields) {
            var data = new URLSearchParams();
            data.set('csrf', csrf);
            Object.keys(fields || {}).forEach(function (key) { data.set(key, String(fields[key])); });
            var response = await fetch(base + path, {
                method: 'POST', credentials: 'same-origin', cache: 'no-store',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' },
                body: data.toString()
            });
            if (response.status === 401 || response.status === 403 || response.redirected) {
                throw new Error('Phiên đăng nhập không hợp lệ. Vui lòng tải lại trang.');
            }
            if (!response.ok) throw new Error('Không thể cập nhật thông báo. Vui lòng thử lại.');
            return response.json();
        }

        function setUnreadCount(count) {
            badgeCount = Number(count) || 0;
            unreadBadge.textContent = badgeCount > 99 ? '99+' : String(badgeCount);
            unreadBadge.hidden = badgeCount <= 0;
            unreadLabel.textContent = badgeCount + ' thông báo chưa đọc';
            var pageUnread = document.getElementById('fr-notif-unread');
            if (pageUnread) pageUnread.textContent = badgeCount > 99 ? '99+' : String(badgeCount);
            var allButton = document.getElementById('fr-notif-read-all');
            if (allButton) allButton.disabled = badgeCount === 0;
        }

        function iconFor(eventType) {
            var type = (eventType || '').toUpperCase();
            if (type.indexOf('ORDER') >= 0) return ['fas fa-shopping-bag', ''];
            if (type.indexOf('PAYMENT') >= 0 || type.indexOf('REFUND') >= 0) return ['fas fa-wallet', 'gold'];
            if (type.indexOf('DELIVER') >= 0 || type.indexOf('SHIP') >= 0) return ['fas fa-truck', 'green'];
            if (type.indexOf('STOCK') >= 0 || type.indexOf('EXPIR') >= 0) return ['fas fa-box-open', 'gold'];
            if (type.indexOf('LISTING') >= 0 || type.indexOf('REVIEW') >= 0) return ['fas fa-clipboard-check', 'green'];
            if (type.indexOf('POINT') >= 0 || type.indexOf('REWARD') >= 0) return ['fas fa-gift', 'gold'];
            return ['far fa-bell', 'blue'];
        }

        function iconNode(eventType) {
            var info = iconFor(eventType);
            var box = make('span', 'fr-notif-card-icon ' + info[1]);
            var i = make('i', info[0]);
            i.setAttribute('aria-hidden', 'true');
            box.appendChild(i);
            return box;
        }

        function dateText(dateString) {
            if (!dateString) return '';
            var d = new Date(dateString);
            if (Number.isNaN(d.getTime())) return '';
            return new Intl.DateTimeFormat('vi-VN', {
                hour: '2-digit', minute: '2-digit',
                day: '2-digit', month: '2-digit', year: 'numeric'
            }).format(d);
        }

        function safeNotificationHref(path) {
            if (typeof path !== 'string' || !path.startsWith('/') || path.startsWith('//') ||
                    path.includes('\\') || /[\r\n\t]/.test(path)) return null;
            // All supplied paths must be application-local, not full URLs.
            return base + path;
        }

        function showMessage(container, message) {
            container.replaceChildren(make('p', 'fr-notif-menu-empty', message));
        }

        async function openItem(item) {
            try {
                if (!item.read) {
                    await postForm('/notifications/read', { id: item.id });
                    item.read = true;
                    await refreshBadge();
                }
            } catch (error) {
                console.warn(error);
            }
            var link = safeNotificationHref(item.linkUrl);
            if (link) window.location.assign(link);
            else {
                await loadMenu();
                if (fullPage) await loadPage();
            }
        }

        function makeMenuItem(item) {
            var button = make('button', 'fr-notif-menu-card');
            button.type = 'button';
            button.appendChild(iconNode(item.eventType));
            var content = make('span', 'fr-notif-menu-copy');
            content.appendChild(make('strong', '', item.title || 'Thông báo'));
            content.appendChild(make('span', 'fr-notif-snippet', item.content || ''));
            content.appendChild(make('span', 'fr-notif-ago', dateText(item.createdAt)));
            button.appendChild(content);
            button.addEventListener('click', function () { openItem(item); });
            return button;
        }

        async function refreshBadge() {
            var result = await getJson('/notifications/unread');
            setUnreadCount(result.unread);
        }

        async function loadMenu() {
            showMessage(menuItems, 'Đang tải thông báo...');
            try {
                var data = await getJson('/notifications/recent?limit=6');
                setUnreadCount(data.unread);
                var rows = Array.isArray(data.notifications) ? data.notifications : [];
                if (!rows.length) showMessage(menuItems, 'Bạn đã xem hết thông báo mới.');
                else menuItems.replaceChildren.apply(menuItems, rows.map(makeMenuItem));
            } catch (error) {
                showMessage(menuItems, error.message);
            }
        }

        function setMenuOpen(open) {
            menu.hidden = !open;
            bell.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (open) loadMenu();
        }

        bell.addEventListener('click', function () { setMenuOpen(menu.hidden); });
        document.addEventListener('click', function (event) {
            if (!root.contains(event.target) && !menu.hidden) setMenuOpen(false);
        });
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape' && !menu.hidden) {
                setMenuOpen(false);
                bell.focus();
            }
        });

        // --------- Full notification page ---------
        var elements = null;
        if (fullPage) {
            elements = {
                items: document.getElementById('fr-notif-page-items'),
                feedback: document.getElementById('fr-notif-feedback'),
                title: document.getElementById('fr-notif-list-title'),
                caption: document.getElementById('fr-notif-list-caption'),
                total: document.getElementById('fr-notif-total'),
                pagination: document.getElementById('fr-notif-pagination'),
                pageNumber: document.getElementById('fr-notif-page-number'),
                prev: document.getElementById('fr-notif-prev'),
                next: document.getElementById('fr-notif-next'),
                readAll: document.getElementById('fr-notif-read-all')
            };
        }

        function setFeedback(text, error) {
            if (!elements) return;
            elements.feedback.hidden = !text;
            elements.feedback.textContent = text || '';
            elements.feedback.classList.toggle('error', !!error);
        }

        function makeEmpty(text) {
            var wrap = make('div', 'fr-notif-empty-state');
            var illustration = make('span', 'fr-notif-empty-illustration');
            illustration.appendChild(make('i', 'far fa-bell-slash'));
            wrap.appendChild(illustration);
            wrap.appendChild(make('h3', '', 'Chưa có thông báo'));
            wrap.appendChild(make('p', '', text));
            return wrap;
        }

        function makeFullEntry(item) {
            var row = make('article', 'fr-notif-entry' + (!item.read ? ' is-unread' : ''));
            row.appendChild(iconNode(item.eventType));
            var body = make('div', 'fr-notif-entry-body');
            var title = make('div', 'fr-notif-entry-title');
            title.appendChild(make('h3', '', item.title || 'Thông báo'));
            if (!item.read) title.appendChild(make('span', 'fr-notif-new-dot'));
            body.appendChild(title);
            body.appendChild(make('p', 'fr-notif-entry-content', item.content || ''));
            var date = make('span', 'fr-notif-entry-date');
            date.appendChild(make('i', 'far fa-clock'));
            date.appendChild(document.createTextNode(dateText(item.createdAt)));
            body.appendChild(date);
            row.appendChild(body);

            var actions = make('div', 'fr-notif-entry-actions');
            if (!item.read) {
                var readButton = make('button', 'fr-notif-read-one', 'Đánh dấu đã đọc');
                readButton.type = 'button';
                readButton.addEventListener('click', async function () {
                    readButton.disabled = true;
                    try {
                        await postForm('/notifications/read', { id: item.id });
                        await refreshBadge();
                        await loadPage();
                    } catch (error) {
                        setFeedback(error.message, true);
                        readButton.disabled = false;
                    }
                });
                actions.appendChild(readButton);
            }
            var href = safeNotificationHref(item.linkUrl);
            if (href) {
                var go = make('a', 'fr-notif-go', 'Xem chi tiết');
                go.href = href;
                go.appendChild(make('i', 'fas fa-arrow-right'));
                go.addEventListener('click', function (event) {
                    if (!item.read && !event.ctrlKey && !event.metaKey && !event.shiftKey && event.button === 0) {
                        event.preventDefault();
                        openItem(item);
                    }
                });
                actions.appendChild(go);
            }
            row.appendChild(actions);
            return row;
        }

        async function loadPage() {
            if (!elements) return;
            var seq = ++pageState.seq;
            elements.items.setAttribute('aria-busy', 'true');
            try {
                var path = pageState.filter === 'unread'
                    ? '/notifications/recent?limit=50'
                    : '/notifications/list?page=' + pageState.page + '&size=10';
                var data = await getJson(path);
                if (seq !== pageState.seq) return;
                setFeedback('', false);
                setUnreadCount(data.unread);
                var rows = Array.isArray(data.notifications) ? data.notifications : [];
                if (pageState.filter === 'all') {
                    pageState.hasMore = Boolean(data.hasMore);
                    elements.total.textContent = 'Đã lưu'; // The API does not expose an all-time count.
                    elements.caption.textContent = 'Thông báo được sắp xếp từ mới đến cũ.';
                } else {
                    pageState.hasMore = false;
                    elements.total.textContent = 'Đã lưu';
                    elements.caption.textContent = 'Hiển thị tối đa 50 thông báo chưa đọc gần nhất.';
                }
                if (!rows.length) {
                    elements.items.replaceChildren(makeEmpty(pageState.filter === 'unread'
                        ? 'Bạn đã đọc tất cả thông báo gần đây.' : 'Khi có hoạt động mới, thông báo sẽ xuất hiện tại đây.'));
                } else {
                    elements.items.replaceChildren.apply(elements.items, rows.map(makeFullEntry));
                }
                elements.pagination.hidden = pageState.filter === 'unread' || (!pageState.hasMore && pageState.page === 0);
                elements.pageNumber.textContent = 'Trang ' + (pageState.page + 1);
                elements.prev.disabled = pageState.page === 0;
                elements.next.disabled = !pageState.hasMore;
            } catch (error) {
                if (seq !== pageState.seq) return;
                elements.items.replaceChildren(makeEmpty('Không thể tải dữ liệu. Vui lòng thử lại.'));
                setFeedback(error.message, true);
            } finally {
                if (seq === pageState.seq) elements.items.setAttribute('aria-busy', 'false');
            }
        }

        if (elements) {
            fullPage.querySelectorAll('[data-filter]').forEach(function (button) {
                button.addEventListener('click', function () {
                    var next = button.dataset.filter;
                    if (next === pageState.filter) return;
                    pageState.filter = next;
                    pageState.page = 0;
                    fullPage.querySelectorAll('[data-filter]').forEach(function (b) {
                        b.classList.toggle('active', b === button);
                        b.setAttribute('aria-pressed', String(b === button));
                    });
                    elements.title.textContent = next === 'unread' ? 'Thông báo chưa đọc' : 'Tất cả thông báo';
                    loadPage();
                });
            });
            elements.prev.addEventListener('click', function () {
                if (pageState.page > 0) { pageState.page--; loadPage(); }
            });
            elements.next.addEventListener('click', function () {
                if (pageState.hasMore) { pageState.page++; loadPage(); }
            });
            elements.readAll.addEventListener('click', async function () {
                if (!badgeCount) return;
                elements.readAll.disabled = true;
                try {
                    await postForm('/notifications/read-all');
                    await refreshBadge();
                    pageState.page = 0;
                    await loadPage();
                } catch (error) {
                    setFeedback(error.message, true);
                    elements.readAll.disabled = false;
                }
            });
            loadPage();
        }

        async function poll() {
            if (polling || document.hidden) return;
            polling = true;
            try {
                await refreshBadge();
                if (!menu.hidden) await loadMenu();
                if (fullPage) await loadPage();
            } catch (error) {
                // Keep the last known count if network/DB is temporarily unavailable.
                console.warn('Freshora notification polling:', error.message);
            } finally {
                polling = false;
            }
        }

        poll();
        window.setInterval(poll, 5000);
        document.addEventListener('visibilitychange', function () {
            if (!document.hidden) poll();
        });
    }

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start);
    else start();
}());
