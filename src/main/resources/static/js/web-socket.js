const socket = new WebSocket(
    (location.protocol === 'https:' ? 'wss://' : 'ws://') +
    location.host +
    '/ws/online-users'
);

socket.onmessage = (event) => {
    document.getElementById("onlineCount").textContent = event.data;
};

