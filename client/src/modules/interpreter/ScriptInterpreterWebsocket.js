import URI from 'urijs';

class ScriptInterpreterWebsocket {
  webSocket;

  closeConnectionFunctionBuilder = (callback) => () => {
    if (this.webSocket) {
      this.webSocket.close();
      this.webSocket = null;

      callback();
    }
  };

  interpreterBuilder = (initialCallback, onMessageCallback, closeConnectionCallback) => (transactionId, inputIndex) =>{
    const uri = new URI({
      protocol: window.location.protocol === 'https:' ? 'wss' : 'ws',
      hostname: window.location.host,
      path: `/api/transaction/${transactionId}/input/${inputIndex}/stream-interpret`
    });

    const closeConnectionFunction = this.closeConnectionFunctionBuilder(closeConnectionCallback);
    closeConnectionFunction();

    initialCallback();

    this.webSocket = new WebSocket(uri.toString());

    this.webSocket.onmessage = event => {
      const interpretResult = JSON.parse(event.data);
      onMessageCallback(interpretResult)
    };

    this.webSocket.onclose = closeConnectionFunction;
  };
}

export default ScriptInterpreterWebsocket;