import _ from 'lodash';

import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import Grid from "@material-ui/core/Grid/Grid";
import {Link} from 'react-router-dom';

class TransactionDetailsComponent extends React.Component {
  render() {
    const {transaction} = this.props;
    const inputsLength = transaction.inputs.length;

    return (
      <div>
        {
          inputsLength < 0 ?
            null :
            <Grid container>
              <Grid item sm={6} xs={12}>
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
                            <TableCell>
                              <div className='btc-address'>
                                {_.head(input.addresses)}
                              </div>
                              <div>
                                <span className='btc spent'>{input.output_value / 100000000} BTC</span> - <a href={ `/transaction/${input.prev_hash}` }> transaction </a>
                              </div>
                            </TableCell>
                          </TableRow>
                        )
                      })
                    }
                  </TableBody>
                </Table>
              </Grid>
              <Grid item sm={6} xs={12}>
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
                            <TableCell>
                              <div className='btc-address'>
                                {_.head(output.addresses)}
                              </div>
                              <div>
                                <span className='btc to'>{output.value / 100000000} BTC</span> - {
                                  output.spent_by ? <a href={ `/transaction/${output.spent_by}` }> transaction </a> : 'not spent yet'
                                }
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