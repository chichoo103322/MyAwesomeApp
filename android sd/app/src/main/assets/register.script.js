// register.script.js
document.addEventListener('DOMContentLoaded', () => {

    const registerForm = document.getElementById('register-form');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const errorMessage = document.getElementById('error-message');

    if (registerForm) {
        registerForm.addEventListener('submit', async (event) => {
            event.preventDefault(); // 阻止表单默认的刷新页面的行为
            errorMessage.textContent = ''; // 清空之前的错误信息

            const username = usernameInput.value.trim();
            const password = passwordInput.value.trim();

            if (username === '' || password === '') {
                errorMessage.textContent = '用户名和密码不能为空！';
                return;
            }

            try {
                // 使用 fetch API 将注册信息发送到后端
                const response = await fetch('http://10.0.2.2:4567/api/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json' // 告诉后端我们发送的是JSON
                    },
                    // 将JavaScript对象转换为JSON字符串
                    body: JSON.stringify({
                        username: username,
                        password: password
                    }),
                    credentials: 'include' // 即使注册不需要，也养成好习惯，为所有API请求加上
                });

                const result = await response.json(); // 解析后端返回的JSON响应

                if (response.ok) { // 如果HTTP状态码是 201 Created
                    alert('注册成功！现在去登录吧。');
                    window.location.href = 'login.html'; // 注册成功后跳转到登录页面
                } else {
                    // 如果后端返回了错误（如409 用户名已存在），在这里显示
                    errorMessage.textContent = result.message;
                }
            } catch (error) {
                console.error('注册请求失败:', error);
                errorMessage.textContent = '注册请求失败，请检查网络或后端服务是否运行。';
            }
        });
    }
});