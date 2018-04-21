import _ from 'lodash';
import React from 'react';

const InterpreterContainer = ({pubKeyScript, sigScript}) => {
  const formattedPubKeyScript = _.join(pubKeyScript, ', ');
  const formattedSigScript = _.join(sigScript, ', ');
  return (
    <div>
      <p><b>pubKeyScript:</b></p>
      <p>{formattedPubKeyScript}</p>
      <p><b>sigScript:</b></p>
      <p>{formattedSigScript}</p>
    </div>
  )
};

export default InterpreterContainer;