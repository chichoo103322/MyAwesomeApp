// chichoo.script.js

// 页面加载时，立即检查用户的登录状态
(async function checkLoginStatus() {
    try {
        console.log('开始检查登录状态...');
        const response = await fetch('http://10.0.2.2:4567/api/status', {
            credentials: 'include',
            method: 'GET'
        });

        console.log('状态检查响应状态:', response.status);

        if (!response.ok) {
            console.log('状态检查失败，重定向到登录页');
            window.location.href = 'login.html';
            return;
        }

        const data = await response.json();
        console.log('状态检查数据:', data);

        if (data.loggedIn) {
            // 等待DOM加载完毕后再更新欢迎信息和加载历史记录
            document.addEventListener('DOMContentLoaded', () => {
                const welcomeMessage = document.getElementById('welcome-message');
                if (welcomeMessage) {
                    welcomeMessage.textContent = `欢迎, ${data.username}`;
                }

                // 调用加载历史记录的函数
                loadChatHistory();
            });
        } else {
            alert('请先登录！');
            window.location.href = 'login.html';
        }
    } catch (error) {
        console.error('检查登录状态时出错:', error);
        alert('无法验证登录状态，请重新登录。');
        window.location.href = 'login.html';
    }
})();

document.addEventListener('DOMContentLoaded', () => {
    // --- 获取所有需要的HTML元素 ---
    const messageInput = document.getElementById('message-input');
    const sendButton = document.getElementById('send-button');
    const chatBox = document.getElementById('chat-box');
    const logoutButton = document.getElementById('logout-button');
    const welcomeMessage = document.getElementById('welcome-message');
    const attachButton = document.getElementById('attach-button');
    const fileInput = document.getElementById('file-input');
    const changePasswordBtn = document.getElementById('change-password-btn');
    const passwordModal = document.getElementById('password-modal');
    const passwordForm = document.getElementById('password-form');
    const cancelPasswordChangeBtn = document.getElementById('cancel-password-change');
    const oldPasswordInput = document.getElementById('old-password');
    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const modelSelector = document.getElementById('model-selector');

    // --- 核心功能函数 ---

    // 新增：加载聊天记录的函数
    async function loadChatHistory() {
        try {
            const response = await fetch('http://10.0.2.2:4567/api/chat/history', {
                credentials: 'include'
            });
            if (!response.ok) {
                console.error('获取聊天记录失败。');
                return;
            }
            const history = await response.json();

            chatBox.innerHTML = ''; // 清空聊天框

            // 遍历历史记录，并使用 appendMessage 显示
            history.forEach(message => {
                const cssClass = message.role === 'user' ? 'user-message' : 'ai-message';
                appendMessage(message.content, cssClass);
            });

        } catch (error) {
            console.error('加载聊天记录时出错:', error);
        }
    }

    async function sendMessage() {
        const userMessageText = messageInput.value.trim();
        if (userMessageText === '') return;

        appendMessage(userMessageText, 'user-message');
        messageInput.value = '';
        messageInput.focus();

        const thinkingMessageDiv = appendMessage('Chichoo 正在思考...', 'ai-message thinking');

        try {
            const selectedModel = modelSelector.value;

            const response = await fetch('http://10.0.2.2:4567/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    message: userMessageText,
                    model: selectedModel
                }),
                credentials: 'include'
            });

            thinkingMessageDiv.remove();

            const responseText = await response.text();

            if (!response.ok) {
                if (response.status === 401) {
                    alert('登录状态已过期，请重新登录');
                    window.location.href = 'login.html';
                    return;
                }
                throw new Error(responseText);
            }

            appendMessage(responseText, 'ai-message');
        } catch (error) {
            console.error('聊天请求错误:', error);
            thinkingMessageDiv.remove();
            appendMessage(`抱歉，出错了: ${error.message}`, 'ai-message');
        }
    }

    async function uploadFile(file) {
        const formData = new FormData();
        formData.append("file", file);
        const selectedModel = modelSelector.value;
        formData.append("model", selectedModel);

        const uploadingMessageDiv = appendMessage(`正在上传并分析文件: ${file.name}...`, 'user-message uploading');

        try {
            const response = await fetch('http://10.0.2.2:4567/api/upload', {
                method: 'POST',
                credentials: 'include',
                body: formData
            });

            uploadingMessageDiv.remove();

            const resultText = await response.text();

            if (response.ok) {
                appendMessage(resultText, 'ai-message');
            } else {
                if (response.status === 401) {
                    alert('登录状态已过期，请重新登录');
                    window.location.href = 'login.html';
                    return;
                }
                throw new Error(resultText);
            }
        } catch (error) {
            console.error('文件上传错误:', error);
            uploadingMessageDiv.remove();
            appendMessage(`文件上传失败: ${error.message}`, 'ai-message');
        }
    }

    function appendMessage(text, className) {
        const messageDiv = document.createElement('div');
        messageDiv.className = `message ${className}`;
        messageDiv.textContent = text;
        chatBox.appendChild(messageDiv);
        chatBox.scrollTop = chatBox.scrollHeight;
        return messageDiv;
    }

    // --- 事件监听器 ---
    sendButton.addEventListener('click', sendMessage);

    messageInput.addEventListener('keypress', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            sendMessage();
        }
    });

    attachButton.addEventListener('click', () => fileInput.click());

    fileInput.addEventListener('change', (event) => {
        const file = event.target.files[0];
        if (file) {
            uploadFile(file);
            event.target.value = '';
        }
    });

    logoutButton.addEventListener('click', async () => {
        try {
            await fetch('http://10.0.2.2:4567/api/logout', {
                credentials: 'include'
            });
            alert('已成功登出！');
            window.location.href = 'login.html';
        } catch (error) {
            console.error('登出时出错:', error);
            alert('登出时发生错误，请稍后再试。');
        }
    });

    changePasswordBtn.addEventListener('click', () => {
        passwordModal.classList.add('visible');
    });

    cancelPasswordChangeBtn.addEventListener('click', () => {
        passwordModal.classList.remove('visible');
        passwordForm.reset();
    });

    passwordForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const oldPassword = oldPasswordInput.value;
        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;
        if (newPassword !== confirmPassword) {
            alert('两次输入的新密码不一致！');
            return;
        }
        if (!newPassword || newPassword.length < 3) {
            alert('新密码太短了！');
            return;
        }
        try {
            const response = await fetch('http://10.0.2.2:4567/api/change-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ oldPassword, newPassword })
            });
            const result = await response.json();
            if (response.ok) {
                alert('密码修改成功！');
                passwordModal.classList.remove('visible');
                passwordForm.reset();
            } else {
                if (response.status === 401 && result.message === '请先登录') {
                    alert('登录状态已过期，请重新登录');
                    window.location.href = 'login.html';
                    return;
                }
                alert(`修改失败: ${result.message}`);
            }
        } catch (error) {
            console.error('修改密码请求失败:', error);
            alert('修改密码时发生网络错误。');
        }
    });

    messageInput.focus();
});