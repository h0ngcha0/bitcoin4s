import _ from 'lodash';

import React from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Grid from "@mui/material/Grid/Grid";
import HomeIcon from '@mui/icons-material/Home';
import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import DebugIcon from '@mui/icons-material/BugReport';
import ScriptOpCodeList from "./ScriptOpCodeList";
import BitcoinIcon from '../../assets/icons/BitcoinIcon';
import Typography from "@mui/material/Typography";
import { Transaction } from '../../api';

interface TransactionDetailsComponentProps {
    transaction: Transaction
}

export const TransactionDetailsComponent: React.FunctionComponent<TransactionDetailsComponentProps> = (props) => {
    const { transaction } = props;
    const inputsLength = transaction.inputs.length;
    return (
        <div style={{ maxWidth: '480px', textAlign: 'center', marginTop: '4px' }}>
            {
                inputsLength < 0 ?
                    null :
                    <Grid container >
                        <Grid item sm={12} xs={12} >
                            <Table padding="none">
                                <TableHead>
                                    <TableRow>
                                        <TableCell style={{ height: "48px" }}>
                                            <Typography color="textSecondary" variant="caption">Paying from</Typography>
                                        </TableCell>
                                    </TableRow>
                                </TableHead>

                                <TableBody>
                                    {
                                        _.map(transaction.inputs, (input, index) => {
                                            return (
                                                <TableRow key={index}>
                                                    <TableCell style={{ whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                                                        <div className='btc-address'>
                                                            <HomeIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span style={{ verticalAlign: "middle" }}> {_.head(input.addresses)} </span>
                                                        </div>
                                                        <div className='btc-amount'>
                                                            <BitcoinIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span className='btc spent'> {input.outputValue / 100000000} BTC</span> - <a href={`/#/transaction/${input.prevHash}`}>Transaction</a>
                                                            {
                                                                input.outputIndex ? <span> output {input.outputIndex} </span> : null
                                                            }
                                                        </div>
                                                        <div>
                                                            <LockOpenIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span> ScriptSig</span> - <ScriptType scriptTypeRaw={input.scriptType} />
                                                        </div>
                                                        <div style={{ marginTop: "5px" }}>
                                                            <ScriptOpCodeList opCodes={input.parsedScript} />
                                                        </div>
                                                        <div style={{ paddingBottom: "10px" }}>
                                                            <DebugIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span> </span>
                                                            <a className="block-link" href={`/#/transaction/${transaction.hash}/input/${index}/interpret?automatic=true`}>Interpret</a> or
                                                            <span> </span><a className="block-link" href={`/#/transaction/${transaction.hash}/input/${index}/interpret?step=0`}>debug</a>
                                                        </div>
                                                    </TableCell>
                                                </TableRow>
                                            )
                                        })
                                    }
                                </TableBody>
                            </Table>
                        </Grid>
                        <Grid item sm={12} xs={12}>
                            <Table padding="none">
                                <TableHead>
                                    <TableRow>
                                        <TableCell style={{ height: "48px" }}>
                                            <Typography color="textSecondary" variant="caption">To</Typography>
                                        </TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {
                                        _.map(transaction.outputs, (output, index) => {
                                            return (
                                                <TableRow key={index}>
                                                    <TableCell style={{ whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                                                        <div className='btc-address'>
                                                            <HomeIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span style={{ verticalAlign: "middle" }}>{_.head(output.addresses) || 'No address'}</span>
                                                        </div>
                                                        <div className='btc-amount'>
                                                            <BitcoinIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span className='btc to'> {output.value / 100000000} BTC</span> - {
                                                                output.spentBy ? <a href={`/#/transaction/${output.spentBy}`}> Transaction </a> : 'not spent yet'
                                                            }
                                                        </div>
                                                        <div>
                                                            <LockIcon style={{ verticalAlign: "middle", fontSize: "18px" }} />
                                                            <span style={{ verticalAlign: "middle" }}> ScriptPubKey</span> - <ScriptType scriptTypeRaw={output.scriptType} />
                                                        </div>
                                                        <div style={{ marginTop: "5px" }}>
                                                            <ScriptOpCodeList opCodes={output.parsedScript} />
                                                        </div>
                                                    </TableCell>

                                                </TableRow>
                                            )
                                        })
                                    }
                                </TableBody>
                            </Table>
                        </Grid>
                    </Grid>
            }
        </div>
    )
};

const ScriptType = ({ scriptTypeRaw }) => {
    if (scriptTypeRaw === "pay-to-pubkey-hash") {
        return (
            <a href="https://en.bitcoinwiki.org/wiki/Pay-to-Pubkey_Hash"> P2PKH </a>
        );
    } if (scriptTypeRaw === "pay-to-script-hash") {
        return (
            <a href="https://en.bitcoinwiki.org/wiki/Pay-to-Script_Hash"> P2SH </a>
        );
    } if (scriptTypeRaw === 'null-data') {
        return (
            <a href="https://btcinformation.org/en/glossary/null-data-transaction"> NULL DATA </a>
        );
    } else if (scriptTypeRaw) {
        return (<span> {scriptTypeRaw} </span>);
    } else {
        return null;
    }
};


export default TransactionDetailsComponent;