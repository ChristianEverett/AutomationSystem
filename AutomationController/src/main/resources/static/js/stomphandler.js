
var sock = new SockJS("/system");
var stomp = Stomp.over(sock);
var onConnectCallback;

function connect(onConnect)
{
    onConnectCallback = onConnect;
    stomp.connect({}, onConnect, onFail);
}

function onFail(error)
{
    sock = new SockJS("/system");
    stomp = Stomp.over(sock);
    connect(onConnectCallback);
}

function register(extension, callback)
{
    stomp.subscribe("/topic/" + extension, function(data)
    {
        var json = JSON.parse(data.body);
        callback(json);
    });
}