import _ from 'lodash';

import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Grid from "@material-ui/core/Grid/Grid";
import HomeIcon from '@material-ui/icons/Home';
import LockIcon from '@material-ui/icons/Lock';
import LockOpenIcon from '@material-ui/icons/LockOpen';
import AttachMoneyIcon from '@material-ui/icons/AttachMoney';
import CodeIcon from '@material-ui/icons/BugReport';
import MoneyOffIcon from '@material-ui/icons/MoneyOff';
import ScriptOpCodeList from "./ScriptOpCodeList";

class TransactionDetailsComponent extends React.Component {
  render() {
    const {transaction} = this.props;
    const inputsLength = transaction.inputs.length;

    return (
      <div style={ {maxWidth: '550px', textAlign: 'center', margin: '0 auto'} }>
        {
          inputsLength < 0 ?
            null :
            <Grid container >
              <Grid item sm={12} xs={12} >
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Paying from</TableCell>
                    </TableRow>
                  </TableHead>

                  <TableBody>
                    {
                      _.map(transaction.inputs, (input, index) => {
                        return (
                          <TableRow key={index}>
                            <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px" }}>
                              <div className='btc-address'>
                                <HomeIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span style={{verticalAlign: "middle"}}> {_.head(input.addresses)} </span>
                              </div>
                              <div>
                                <MoneyOffIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span className='btc spent'> {input.output_value / 100000000} BTC</span> - <a href={ `/#/transaction/${input.prev_hash}` }>Transaction</a>
                                {
                                  input.output_index ? <span> output {input.output_index} </span> : null
                                }
                              </div>
                              <div>
                                <LockOpenIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span> Unlocking script</span> - <ScriptType scriptTypeRaw={input.script_type}/>
                              </div>
                              <div style={ {marginTop: "5px"}}>
                                <ScriptOpCodeList opCodes={input.parsed_script} />
                              </div>
                              <div>
                                <CodeIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span> </span>
                                <a className="block-link" href={ `/#/transaction/${transaction.hash}/input/${index}/interpret?automatic=true`}>Interpret</a> or debug
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
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>To</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {
                      _.map(transaction.outputs, (output, index) => {
                        return (
                          <TableRow key={index}>
                            <TableCell style={ { whiteSpace: "normal", wordWrap: "break-word", maxWidth: "120px"}}>
                              <div className='btc-address'>
                                <HomeIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span style={{verticalAlign: "middle"}}>{_.head(output.addresses)}</span>
                              </div>
                              <div>
                                <AttachMoneyIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span className='btc to'>{output.value / 100000000} BTC</span> - {
                                  output.spent_by ? <a href={ `/#/transaction/${output.spent_by}` }> Transaction </a> : 'not spent yet'
                                }
                              </div>
                              <div>
                                <LockIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span style={{verticalAlign: "middle"}}> Locking script</span> - <ScriptType scriptTypeRaw={output.script_type}/>
                              </div>
                              <div style={ {marginTop: "5px"}}>
                                <ScriptOpCodeList opCodes={output.parsed_script} />
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
  }
};

const ScriptType = ({scriptTypeRaw}) => {
  if (scriptTypeRaw === "pay-to-pubkey-hash") {
    return (
      <a href="https://en.bitcoinwiki.org/wiki/Pay-to-Pubkey_Hash"> P2PKH </a>
    );
  } if(scriptTypeRaw === "pay-to-script-hash") {
    return (
      <a href="https://en.bitcoinwiki.org/wiki/Pay-to-Script_Hash"> P2SH </a>
    );
  } else if (scriptTypeRaw){
    return (<span> {scriptTypeRaw} </span>);
  } else {
    return null;
  }
};


export default TransactionDetailsComponent;