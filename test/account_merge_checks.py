import urllib.request,urllib.parse,http.cookiejar,re,time
BASE='http://localhost:10109/Freshora'
client=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
def get(path):
 r=client.open(BASE+path);return r.read().decode(),r.url
def post(path,data):
 body,_=get(path);token=re.search(r'name="csrf" value="([^"]+)"',body)[1]
 r=client.open(BASE+path,urllib.parse.urlencode(dict(data,csrf=token)).encode());return r.read().decode(),r.url
def check(ok,msg):
 assert ok,msg
 print('PASS',msg,flush=True)
email='merge-account-'+str(time.time_ns())+'@test.invalid'
post('/register/customer',dict(email=email,password='TestOnly@123',confirm='TestOnly@123',name='Kiểm thử',phone='0900000000'))
post('/login',dict(email=email,password='TestOnly@123'))
body,_=post('/account',dict(action='profile',name='Tên cập nhật',email=email,phone='0912 345.678'))
check('Tên cập nhật' in body and '0912345678' in body,'profile saves and session name refreshes')
body,_=post('/account',dict(action='profile',name='Giữ tên đã nhập',email='invalid',phone='abc'))
check('Giữ tên đã nhập' in body and 'Vui lòng nhập địa chỉ thư điện tử hợp lệ.' in body and 'Vui lòng nhập số điện thoại hợp lệ.' in body,'profile field errors preserve input in typed form')
fields=dict(action='saveAddress',name='Người nhận',phone='0900000000',line='Địa chỉ mới',ward='Phường',district='Quận',citySelect='__other',cityOther='')
body,_=post('/account',fields)
check('Tỉnh/thành phố là bắt buộc.' in body and 'Địa chỉ mới' in body,'address errors preserve typed form and other-city flag')
fields['cityOther']='Tỉnh khác'
body,_=post('/account',fields)
check('Tỉnh khác' in body and 'Địa chỉ mặc định' in body,'first address defaults and custom city renders')
aid=re.search(r'name="id" value="(\d+)"',body)[1]
body,_=post('/account',dict(fields,id=aid,line='Địa chỉ sửa',citySelect='Hà Nội'))
check('Địa chỉ sửa' in body and 'Địa chỉ mặc định' in body,'edit keeps default and renders persisted Address bean')
body,_=post('/account',dict(action='deleteAddress',id=aid))
check('Đã xóa địa chỉ thành công.' in body,'delete address success in Vietnamese')
body,_=post('/account',dict(action='password',old='TestOnly@123',password='TestOnly@123',confirm='TestOnly@123'))
check('Mật khẩu mới phải khác mật khẩu hiện tại.' in body and 'value="TestOnly@123"' not in body,'password error never echoes secrets')
body,url=post('/account',dict(action='password',old='TestOnly@123',password='ChangedPass123!',confirm='ChangedPass123!'))
check(url.endswith('/login') and 'Mật khẩu của bạn đã được cập nhật.' in body,'password success redirects to login with flash')
print('8 account merge HTTP checks passed')
