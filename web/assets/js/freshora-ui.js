const nav=document.getElementById('nav'),btt=document.getElementById('btt'),overlay=document.getElementById('searchOv'),searchButton=document.getElementById('navSearchBtn');
window.addEventListener('scroll',()=>{nav.classList.toggle('scrolled',scrollY>60);btt.classList.toggle('show',scrollY>300);});
btt.addEventListener('click',()=>window.scrollTo({top:0,behavior:'smooth'}));
function closeSearch(){overlay.classList.remove('open');overlay.hidden=true;document.body.style.overflow='';searchButton.focus();}
searchButton.addEventListener('click',()=>{overlay.hidden=false;overlay.classList.add('open');document.body.style.overflow='hidden';document.getElementById('searchInput').focus();});
document.getElementById('searchClose').addEventListener('click',closeSearch);
overlay.addEventListener('click',e=>{if(e.target===overlay)closeSearch();});
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&!overlay.hidden)closeSearch();});
document.querySelectorAll('#nav .nav-link').forEach(a=>{if(a.href&&new URL(a.href).pathname===location.pathname)a.classList.add('active');});
