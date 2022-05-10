import React, { useEffect, useState } from 'react';

import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import btcRpcExplorerImage from '../../assets/images/btc-rpc-explorer.jpg';
import { fetchTransaction } from '../../api';
import TransactionDetailsComponent from "./TransactionDetailsComponent";
import Loading from "../Loading";
import { PurpleColorButton } from "../PurpleColorButton";
import SearchBar from 'material-ui-search-bar';
import BitcoinIcon from '../../assets/icons/BitcoinIcon';
import RawIcon from '../../assets/icons/RawIcon';
import ScriptIcon from '../../assets/icons/ScriptIcon';
import { Typography } from '@material-ui/core';
import ScrollableTabs from "../ScrollableTabs";
import TransactionRawComponent from "./TransactionRawComponent";
import { Transaction } from '../../api';

interface TransactionContainerProps {
    transactionId: string,
    push: (x: any, y: any) => void
}

interface TransactionContainerState {
    transactionId: string,
    transaction: Transaction | undefined,
    loading: boolean,
    error: string | undefined,
    executingScript: boolean
}

export const TransactionContainer: React.FunctionComponent<TransactionContainerProps> = (props) => {
    const [state, setState] = useState<TransactionContainerState>({
        transactionId: props.transactionId,
        transaction: undefined,
        loading: false,
        error: undefined,
        executingScript: false
    });

    const loadTransaction = (transactionId: string) => {
        setState({
            ...state,
            transaction: undefined,
            transactionId: transactionId,
            loading: true
        });

        fetchTransaction(transactionId)
            .then((transaction) => {
                setState({
                    ...state,
                    loading: false,
                    error: undefined,
                    transaction: transaction
                });
            })
            .catch((error) => {
                setState({
                    ...state,
                    loading: false,
                    error: error.response
                });
            });
    };

    useEffect(() => {
        if (props.transactionId) {
            loadTransaction(props.transactionId);
        }
    }, [props.transactionId]);

    const handleSetTransactionId = (txId) => {
        setState({
            ...state,
            transactionId: txId
        });
    };

    const executeAfterFetchTransaction = (state, func) => {
        if (state.error) {
            if (state.error.status === 404) {
                return (
                    <div style={{ marginTop: '32px', textAlign: 'center' }}> 404, <BitcoinIcon style={{ verticalAlign: "middle", fontSize: "200px" }} /> transaction not found </div>
                );
            } else {
                return <div style={{ marginTop: '32px', textAlign: 'center' }}>{state.error.status},  {state.error.statusText}</div>;
            }
        } else if (state.transaction) {
            return func(state.transaction);
        }
    };

    const showRawTransaction = () => {
        return executeAfterFetchTransaction(state, () => (
            <TransactionRawComponent transaction={state.transaction} />
        ));
    };

    const showTransactionDetails = () => {
        return executeAfterFetchTransaction(state, (transaction: Transaction) => (
            <span>
                <TransactionDetailsComponent transaction={transaction} />
                <div style={{ marginTop: '36px', textAlign: 'center' }}>
                    <a className="image" href="https://explorer.nioctib.tech">
                        <img src={btcRpcExplorerImage} className={`explorer-image-mobile img-responsive mobile`} alt="Bitcoin RPC Explorer" />
                        <img src={btcRpcExplorerImage} className={`explorer-image-desktop img-responsive desktop`} alt="Bitcoin RPC Explorer" />
                    </a>
                </div>
                <span>
                    <Typography color="textSecondary" variant="caption">
                        BTC RPC EXPLORER
                    </Typography>
                </span>
            </span>
        ));
    };

    return (
        <div className="container">
            <div className={'application-definition'}>
                <img src={mobileLogoImage} className={`logo-image img-responsive mobile`} alt="Bitcoin Playground" />
                <img src={desktopLogoImage} className={`logo-image img-responsive desktop`} alt="Bitcoin Playground" />
                <SearchBar
                    value={state.transactionId}
                    placeholder="BTC transaction id"
                    onChange={(newValue) => handleSetTransactionId(newValue)}
                    disabled={state.executingScript}
                    style={{ maxWidth: '500px', textAlign: 'center', margin: '0 auto' }}
                    onRequestSearch={() => {
                        loadTransaction(state.transactionId);
                    }}
                />
                <div style={{ marginTop: '16px', textAlign: 'center' }}>
                    {
                        state.loading ?
                            <Loading /> :
                            (
                                <span>
                                    <span>
                                        <PurpleColorButton variant="outlined" size="small" disabled={state.executingScript} onClick={() =>
                                            loadTransaction(state.transactionId)
                                        }>
                                            Search
                                        </PurpleColorButton>
                                    </span>

                                    <div style={{ maxWidth: '480px', textAlign: 'center', margin: 'auto', marginTop: '16px' }}>

                                        <ScrollableTabs tabs={
                                            [
                                                { title: (<ScriptIcon />), children: showTransactionDetails() },
                                                { title: (<RawIcon />), children: showRawTransaction() }
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