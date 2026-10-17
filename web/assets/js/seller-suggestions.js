document.querySelectorAll('[data-suggestions]').forEach(input => {
    const list = document.createElement('div');
    list.setAttribute('aria-live', 'polite');
    (input.closest('label') || input).after(list);
    let timer, request;
    input.addEventListener('input', () => {
        clearTimeout(timer);
        if (request) request.abort();
        list.replaceChildren();
        const query = input.value.trim();
        if (query.length < 2) return;
        timer = setTimeout(async () => {
            const current = new AbortController(); request = current;
            try {
                const response = await fetch(input.dataset.suggestions + '?q=' + encodeURIComponent(query), {signal: current.signal});
                if (!response.ok) throw new Error('Unavailable');
                const rows = await response.json();
                if (current.signal.aborted) return;
                list.textContent = rows.length ? 'Cửa hàng khác — giá từ cao xuống thấp:' : 'Không tìm thấy sản phẩm tương tự.';
                rows.forEach(row => {
                    const line = document.createElement('div');
                    const link = document.createElement('a');
                    link.href = input.dataset.productUrl + '?id=' + row.id;
                    link.textContent = `${row.title} \u00b7 ${row.store} \u00b7 ${row.pack} \u00b7 ${Number(row.price).toLocaleString('vi-VN')} đ` +
                        (row.perKg == null ? '' : ` \u00b7 ${Number(row.perKg).toLocaleString('vi-VN')} đ/kg`);
                    line.append(link);list.append(line);
                });
            } catch (error) { if (error.name !== 'AbortError') list.textContent = 'Tạm thời không thể tải gợi ý sản phẩm.'; }
        }, 250);
    });
});
