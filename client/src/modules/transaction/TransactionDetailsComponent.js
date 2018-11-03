import _ from 'lodash';

import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Grid from "@material-ui/core/Grid/Grid";
import HomeIcon from '@material-ui/icons/Home';
import CodeIcon from '@material-ui/icons/Code';
import AttachMoneyIcon from '@material-ui/icons/AttachMoney';
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
                                <span className='btc spent'> {input.output_value / 100000000} BTC</span> - <a href={ `/#/transaction/${input.prev_hash}` }> transaction </a>
                              </div>
                              <div>
                                <CodeIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span> Script</span> - <a className="block-link" href={ `/#/transaction/${transaction.hash}/input/${index}/interpret`}> interpret</a>
                              </div>
                              <div style={ {marginTop: "5px"}}>
                                <ScriptOpCodeList opCodes={input.parsed_script} />
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
                                  output.spent_by ? <a href={ `/#/transaction/${output.spent_by}` }> transaction </a> : 'not spent yet'
                                }
                              </div>
                              <div>
                                <CodeIcon style={{verticalAlign: "middle", fontSize: "16px"}}/>
                                <span style={{verticalAlign: "middle"}}> Script</span>
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

export default TransactionDetailsComponent;