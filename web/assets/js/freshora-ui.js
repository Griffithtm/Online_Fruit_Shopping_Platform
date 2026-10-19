const nav=document.getElementById('nav'),btt=document.getElementById('btt'),overlay=document.getElementById('searchOv'),searchButton=document.getElementById('navSearchBtn');
window.addEventListener('scroll',()=>{nav.classList.toggle('scrolled',scrollY>60);btt.classList.toggle('show',scrollY>300);});
btt.addEventListener('click',()=>window.scrollTo({top:0,behavior:'smooth'}));
function closeSearch(){overlay.classList.remove('open');overlay.hidden=true;document.body.style.overflow='';searchButton.focus();}
searchButton.addEventListener('click',()=>{overlay.hidden=false;overlay.classList.add('open');document.body.style.overflow='hidden';document.getElementById('searchInput').focus();});
document.getElementById('searchClose').addEventListener('click',closeSearch);
overlay.addEventListener('click',e=>{if(e.target===overlay)closeSearch();});
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!overlay.hidden)closeSearch();});
document.querySelectorAll('#nav .nav-link').forEach(a=>{if(a.href&&new URL(a.href).pathname===location.pathname)a.classList.add('active');});

document.addEventListener('invalid', event => {
    const input = event.target;
    if (!input.validity) return;
    input.setCustomValidity('');
    const v = input.validity;
    let message = 'Giá trị không hợp lệ. Vui lòng kiểm tra lại.';
    if (v.valueMissing) message = 'Vui lòng điền thông tin bắt buộc.';
    else if (v.typeMismatch) message = input.type === 'email' ? 'Vui lòng nhập địa chỉ thư điện tử hợp lệ.' : 'Vui lòng nhập đúng định dạng.';
    else if (v.rangeUnderflow) message = 'Giá trị tối thiểu là ' + input.min + '.';
    else if (v.rangeOverflow) message = 'Giá trị tối đa là ' + input.max + '.';
    else if (v.stepMismatch || v.badInput) message = 'Vui lòng nhập số hợp lệ theo bước cho phép.';
    else if (v.tooShort || v.tooLong) message = 'Độ dài thông tin chưa phù hợp.';
    input.setCustomValidity(message);
}, true);
document.addEventListener('input', event => { if (event.target.setCustomValidity) event.target.setCustomValidity(''); });
document.addEventListener('change', event => { if (event.target.setCustomValidity) event.target.setCustomValidity(''); });

document.querySelectorAll('input[type="file"]').forEach((input, index) => {
    input.id ||= 'tep-tai-len-' + index;
    const picker = document.createElement('span');
    const label = document.createElement('label');
    label.htmlFor = input.id; label.className = 'button'; label.textContent = 'Chọn tệp';
    const info = document.createElement('span'); info.textContent = ' Chưa chọn tệp';
    info.setAttribute('aria-live','polite');
    input.classList.add('visually-hidden');
    input.addEventListener('change', () => { info.textContent = input.files.length ? ' Đã chọn ' + input.files.length + ' tệp' : ' Chưa chọn tệp'; });
    input.addEventListener('focus', () => label.style.outline = '2px solid currentColor');
    input.addEventListener('blur', () => label.style.outline = '');
    picker.append(label,info); input.after(picker);
});
