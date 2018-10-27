import React from 'react';
import {Button} from '@material-ui/core';
import InterpreterComponent from "./InterpreterComponent";
import URI from 'urijs';
import {interpretTransactionInput} from '../../api';
import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import Loading from '../Loading';
import SearchBar from 'material-ui-search-bar'

class InterpreterContainer extends React.Component {
  static propTypes = {};

  state = {
    interpretResult: undefined,
    inputIndex: 0,
    transactionId: "", // 85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac
    loading: false,
    executingScript: false
  };

  handleSetTransactionId = (txId) => {
    this.setState({
      ...this.state,
      transactionId: txId
    });
  };

  handleSetTransactionInputIndex = (inputIndex) => {
    this.setState({
      ...this.state,
      inputIndex: inputIndex
    });
  };

  interpretScript = () => {
    this.setState({
      ...this.state,
      interpretResult: undefined,
      loading: true
    });

    interpretTransactionInput(this.state.transactionId, this.state.inputIndex)
      .then((interpretResponse) => {
        this.setState({
          ...this.state,
          loading: false,
          interpretResult: interpretResponse
        });
      })
      .catch((error) => {
        // TODO: handle error
        console.log(error);
      });
  };

  webSocket;

  closeConnection = () => {
    if (this.webSocket) {
      this.webSocket.close();
      this.webSocket = null;

      this.setState({
        ...this.state,
        executingScript: false
      });
    }
  };

  interpretScriptWebsocket = () => {
    const uri = new URI({
      protocol: window.location.protocol === 'https:' ? 'wss' : 'ws',
      hostname: window.location.host,
      path: `/api/transaction/${this.state.transactionId}/input/${this.state.inputIndex}/stream-interpret`
    });

    this.closeConnection();

    this.setState({
      ...this.state,
      interpretResult: undefined,
      loading: true,
      executingScript: true
    });

    this.webSocket = new WebSocket(uri.toString());

    this.webSocket.onmessage = event => {
      const interpretResult = JSON.parse(event.data);

      this.setState({
        ...this.state,
        interpretResult: interpretResult,
        loading: false
      });
    };

    this.webSocket.onclose = this.closeConnection;
  };

  render() {
    return (
      <div className="container">
        <div className={'application-definition'}>
          <img src={ mobileLogoImage } className={`logo-image img-responsive mobile`} alt="Bitcoin Playground"/>
          <img src={ desktopLogoImage } className={`logo-image img-responsive desktop`} alt="Bitcoin Playground"/>
          <SearchBar
            value={this.state.transactionId}
            placeholder="BTC transaction id"
            onChange={(newValue) => this.handleSetTransactionId(newValue)}
            disabled={ this.state.executingScript }
            onRequestSearch={() => {
              this.interpretScriptWebsocket();
            }}
          />
          <div style={ {marginTop: '16px', textAlign: 'center'} }>
            {
              this.state.loading ?
                <Loading /> :
                <Button variant="contained" color="primary" disabled={ this.state.executingScript } onClick={ () => this.interpretScriptWebsocket()}>
                  Search
                </Button>

            }
          </div>
          {
            this.state.interpretResult ?
              <InterpreterComponent interpretResult={this.state.interpretResult} /> : null
          }
        </div>
      </div>
    );
  }
}

export default InterpreterContainer;