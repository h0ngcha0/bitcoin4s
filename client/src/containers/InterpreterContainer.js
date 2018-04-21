import React from 'react';
import {TextField, RaisedButton, Paper, Subheader} from 'material-ui';
import InterpreterComponent from "../components/InterpreterComponent";

import {interpretTransactionInput} from '../api';

export default class InterpreterContainer extends React.Component {
  static propTypes = {};

  state = {
    interpretResult: undefined,
    inputIndex: 0,
    transactionId: "85db1042f083a8fd6f96fd1a76dc7b8373df9f434979bdcf2432ecf9e0c212ac"
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

  interpreterScript = () => {
    interpretTransactionInput(this.state.transactionId, this.state.inputIndex)
      .then((interpretResponse) => {
        this.setState({
          ...this.state,
          interpretResult: interpretResponse
        });
      })
      .catch((error) => {
        // TODO: handle error
        console.log(error);
      });
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
              this.interpreterScript();
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
            <div>
              <RaisedButton primary type="submit" label="Interpret" />
            </div>
          </form>
          {this.state.interpretResult ? <InterpreterComponent interpretResult={this.state.interpretResult} /> : null}
        </Paper>
      </div>
    );
  }

  componentDidMount() {
  }
}
