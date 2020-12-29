import React from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import btcRpcExplorerImage from '../../assets/images/btc-rpc-explorer.jpg';
import {fetchTransaction} from '../../api';
import TransactionDetailsComponent from "./TransactionDetailsComponent";
import Loading from "../Loading";
import {PurpleColorButton, BlueColorButton} from "../PurpleColorButton";
import SearchBar from 'material-ui-search-bar';
import BitcoinIcon from '../../assets/icons/BitcoinIcon';
import RawIcon from '../../assets/icons/RawIcon';
import ScriptIcon from '../../assets/icons/ScriptIcon';
import ScrollableTabs from "../ScrollableTabs";
import TransactionRawComponent from "./TransactionRawComponent";

export default class TransactionContainer extends React.Component {
  componentWillMount() {
    this.setState({
      transactionId: this.props.transactionId,
      transaction: undefined
    });
  }

  componentDidMount() {
    if (this.props.transactionId) {
      this.loadTransaction(this.props.transactionId);
    }
  }

  componentWillReceiveProps(nextProps, _) {
    if (this.props.transactionId !== nextProps.transactionId) {
      this.loadTransaction(nextProps.transactionId);
    }
  }

  handleSetTransactionId = (txId) => {
    this.setState({
      ...this.state,
      transactionId: txId
    });
  };

  loadTransaction = (transactionId) => {
    this.setState({
      ...this.state,
      transaction: undefined,
      interpretResult: undefined,
      transactionId: transactionId,
      loading: true
    });

    fetchTransaction(transactionId)
      .then((transaction) => {
        this.setState({
          ...this.state,
          loading: false,
          error: undefined,
          transaction: transaction
        });
      })
      .catch((error) => {
        this.setState({
          ...this.state,
          loading: false,
          error: error.response
        });
      });
  };

  executeAfterFetchTransaction = (state, func) => {
    if (state.error) {
      if (state.error.status === 404) {
        return (
            <div style={ {marginTop: '32px', textAlign: 'center'} }> 404, <BitcoinIcon style={{verticalAlign: "middle", fontSize: "200px"}}/> transaction not found </div>
        );
      } else {
        return <div style={ {marginTop: '32px', textAlign: 'center'} }>{state.error.status},  {state.error.statusText}</div>;
      }
    } else if (state.transaction) {
      return func();
    }
  };

  showRawTransaction = () => {
    return this.executeAfterFetchTransaction(this.state, () => (
        <TransactionRawComponent transaction={this.state.transaction}/>
    ));
  };

  showTransactionDetails = () => {
    return this.executeAfterFetchTransaction(this.state, () => (
        <span>
          <TransactionDetailsComponent transaction={this.state.transaction} />
          <div style={ {marginTop: '36px', textAlign: 'center'} }>
            <a className="image" href="https://explorer.nioctib.tech">
              <img src={ btcRpcExplorerImage } className={ `explorer-image-mobile img-responsive mobile` } alt="Bitcoin RPC Explorer">
              </img>
              <img src={ btcRpcExplorerImage } className={ `explorer-image-desktop img-responsive desktop` } alt="Bitcoin RPC Explorer">
              </img>
            </a>
          </div>
        </span>
    ));
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
            style={ {maxWidth: '500px', textAlign: 'center', margin: '0 auto'} }
            onRequestSearch={() => {
              this.loadTransaction(this.state.transactionId);
            }}
          />
          <div style={ {marginTop: '16px', textAlign: 'center'} }>
            {
              this.state.loading ?
                <Loading /> :
                (
                  <span>
                    <span>
                      <PurpleColorButton variant="outlined" size="small" disabled={ this.state.executingScript } onClick={ () =>
                          this.loadTransaction(this.state.transactionId)
                      }>
                        Search
                      </PurpleColorButton>
                    </span>

                    <div style={ {maxWidth: '480px', textAlign: 'center', margin: 'auto', marginTop: '16px'} }>

                      <ScrollableTabs tabs ={
                        [
                          {title: (<ScriptIcon />), children: this.showTransactionDetails()},
                          {title: (<RawIcon />), children: this.showRawTransaction()}
                        ]
                      } />
                    </div>
                  </span>
                )
            }
          </div>
       </div>

      </div>
    );
  }
}