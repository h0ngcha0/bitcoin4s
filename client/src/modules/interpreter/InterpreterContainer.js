import React from 'react';
import {Button} from '@material-ui/core';
import InterpreterComponent from "./InterpreterComponent";
import {interpretTransactionInput} from '../../api';
import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import Loading from '../Loading';
import SearchBar from 'material-ui-search-bar'
import ScriptInterpreterWebsocket from './ScriptInterpreterWebsocket';

// Idea, show the link of a few typical tx, such as p2sh, p2pkh, multi-sig, etc
// Move web socket out

class InterpreterContainer extends React.Component {
  static propTypes = {};

  state = {
    interpretResult: undefined,
    inputIndex: 0,
    transactionId: "85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac", // 85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac
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

  initialCallback = () => {
    this.setState({
      ...this.state,
      interpretResult: undefined,
      loading: true,
      executingScript: true
    });
  };

  closeConnectionCallback = () => {
    this.setState({
      ...this.state,
      executingScript: false
    });
  };

  onMessageCallback = (interpretResult) => {
    this.setState({
      ...this.state,
      interpretResult: interpretResult,
      loading: false
    });
  };

  scriptInterpreterWebsocket = new ScriptInterpreterWebsocket();
  scriptInterpreter = this.scriptInterpreterWebsocket.interpreterBuilder(this.initialCallback, this.onMessageCallback, this.closeConnectionCallback);

  interpretScriptWebsocket = (transactionId, inputIndex) => {
    this.scriptInterpreter(transactionId, inputIndex)
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
              this.interpretScriptWebsocket(this.state.transactionId, this.state.inputIndex);
            }}
          />
          <div style={ {marginTop: '16px', textAlign: 'center'} }>
            {
              this.state.loading ?
                <Loading /> :
                <Button variant="contained" color="primary" disabled={ this.state.executingScript } onClick={ () =>
                  this.interpretScriptWebsocket(this.state.transactionId, this.state.inputIndex)
                }>
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