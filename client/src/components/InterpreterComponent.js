import _ from 'lodash';
import React from 'react';

const InterpreterContainer = ({interpretResult}) => {
  const {scriptPubKey, scriptSig, currentScript, stack, altStack} = interpretResult.state;
  const getType = (op) => op.type;
  const formatScript = (array) => '[ ' + _.join(_.map(array, getType), ', ') + ' ]';

  const formattedPubKeyScript = formatScript(scriptPubKey);
  const formattedSigScript = formatScript(scriptSig);
  const formattedCurrentScript = formatScript(currentScript);
  const formattedStack = formatScript(stack);
  const formattedAltStack = formatScript(altStack);

  const result = interpretResult.result.type === 'Result' ? (interpretResult.result.value ? 'True' : 'False') : 'NoResult';

  return (
    <div>
      <p><b>Result:</b></p>
      <p>{result}</p>
      <p><b>ScriptPubKey:</b></p>
      <p>{formattedPubKeyScript}</p>
      <p><b>ScriptSig:</b></p>
      <p>{formattedSigScript}</p>
      <p><b>Current Script:</b></p>
      <p>{formattedCurrentScript}</p>
      <p><b>Current Stack:</b></p>
      <p>{formattedStack}</p>
      <p><b>Current Alt Stack:</b></p>
      <p>{formattedAltStack}</p>
    </div>
  )
};

export default InterpreterContainer;