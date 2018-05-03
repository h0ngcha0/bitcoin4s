import React from 'react';
import {TextField, RaisedButton, Paper, Subheader} from 'material-ui';
import InterpreterComponent from "./InterpreterComponent";
import URI from 'urijs';
import {interpretTransactionInput} from '../../api';
import { GridLoader } from 'react-spinners';
import muiThemeable from 'material-ui/styles/muiThemeable';

class InterpreterContainer extends React.Component {
  static propTypes = {};

  state = {
    interpretResult: undefined,
    inputIndex: 0,
    transactionId: "85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac",
    loading: false
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
    }
  };

  interpretScriptWebsocket = () => {
    const uri = new URI({
      protocol: window.location.protocol === 'https:' ? 'wss' : 'ws',
      hostname: window.location.host,
      path: `/transaction/${this.state.transactionId}/input/${this.state.inputIndex}/stream-interpret`
    });

    this.closeConnection();

    this.webSocket = new WebSocket(uri.toString());

    this.webSocket.onmessage = event => {
      const interpretResult = JSON.parse(event.data);

      this.setState({
        ...this.state,
        interpretResult: interpretResult
      });
    };

    this.webSocket.onclose = this.closeConnection;
  };

  render() {

    return (
      <div className="container">
        <Paper zDepth={1} className={'application-definition'}>
          <Subheader style={{paddingLeft: 0}}>Bitcoin Script Interpreter</Subheader>
          <form
            className="container"
            onSubmit={ (event) => {
              event.preventDefault();
              event.stopPropagation();
              this.interpretScriptWebsocket();
            }}
            noValidate
            autoComplete="off"
          >
            <div>
              <TextField
                id="transactionId"
                floatingLabelText="Transaction Id"
                value={this.state.transactionId}
                onChange={ (event) => {this.handleSetTransactionId(event.target.value)} }
                InputLabelProps={{
                  shrink: true,
                }}
                margin="normal"
              />
            </div>
            <div>
              <TextField
                id="number"
                floatingLabelText="Input Index"
                value={this.state.inputIndex}
                onChange={ (event) => {this.handleSetTransactionInputIndex(event.target.value)} }
                type="number"
                InputLabelProps={{
                  shrink: true,
                }}
                margin="normal"
              />
            </div>
            <div style={ {'margin-top': '16px'} }>
              {
                true ?
                  <GridLoader
                    color={ this.props.muiTheme.palette.primary1Color }
                    loading={ true }
                  /> :
                  <RaisedButton primary type="submit" label="Interpret" />
              }
            </div>
          </form>
          {
            this.state.interpretResult ?
              <InterpreterComponent interpretResult={this.state.interpretResult} /> : null

          }
        </Paper>
      </div>
    );
  }

  componentDidMount() {
  }
}

export default muiThemeable()(InterpreterContainer);