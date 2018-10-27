import _ from 'lodash';

import React from 'react';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';

class TransactionDetailsComponent extends React.Component {

  state = {
    isVisible: true
  };

  render() {
    const {transaction} = this.props;
    const inputsLength = transaction.inputs.length;

    return (
      <div>
        {
          inputsLength < 0 ?
            null :
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
                        <TableCell>{_.head(input.addresses)}</TableCell>
                      </TableRow>
                    )
                  })
                }
              </TableBody>
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
                        <TableCell>{_.head(output.addresses)}</TableCell>
                      </TableRow>
                    )
                  })
                }
              </TableBody>
            </Table>
        }
      </div>
    )
  }
};

export default TransactionDetailsComponent;