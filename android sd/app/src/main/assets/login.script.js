// login.script.js
document.addEventListener('DOMContentLoaded', () => {

    const loginForm = document.getElementById('login-form');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const errorMessage = document.getElementById('error-message');

    if (loginForm) {
        loginForm.addEventListener('submit', async (event) => {
            event.preventDefault(); // 阻止表单默认的刷新页面的行为
            errorMessage.textContent = ''; // 清空之前的错误信息

            const username = usernameInput.value.trim();
            const password = passwordInput.value.trim();

            if (username === '' || password === '') {
                errorMessage.textContent = '用户名和密码不能为空！';
                return;
            }

            try {
                // 使用 fetch API 将登录信息发送到后端
                const response = await fetch('http://10.0.2.2:4567/api/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        username: username,
                        password: password
                    }),
                    credentials: 'include' // 关键！发送跨域请求时必须带上这个才能传递Cookie
                });

                const result = await response.json();

                if (response.ok) { // 如果HTTP状态码是 200 OK
                    alert('登录成功！');
                    // 登录成功后，跳转到 Chichoo 聊天主页面
                    window.location.href = 'chichoo.html'; 
                } else {
                    // 如果登录失败，显示后端返回的错误信息（如“用户名或密码错误”）
                    errorMessage.textContent = result.message;
                }
            } catch (error) {
                console.error('登录请求失败:', error);
                errorMessage.textContent = '登录请求失败，请检查网络或后端服务是否运行。';
            }
        });
    }
});