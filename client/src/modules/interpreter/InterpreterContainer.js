import React from 'react';

import InterpreterComponent from "./InterpreterComponent";
import {interpretTransactionInputWithSteps} from '../../api';
import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import Loading from '../Loading';
import NavigateBeforeIcon from '@material-ui/icons/NavigateBefore';
import NavigateNextIcon from '@material-ui/icons/NavigateNext';
import ScriptInterpreterWebsocket from './ScriptInterpreterWebsocket';
import Grid from "@material-ui/core/Grid/Grid";

class InterpreterContainer extends React.Component {
  static propTypes = {};

  componentDidMount() {
    if (this.state.transactionId && this.state.inputIndex) {
      if (this.state.automatic) {
        this.interpretScriptWebsocket();
      } else {
        const step = this.props.step ? this.props.step : 0;
        this.interpretScript(step);
      }
    }
  }

  componentDidUpdate(prevProps) {
    if (!this.props.automatic && (this.props.step !== prevProps.step)) {
      this.interpretScript(this.props.step);
    }
  }

  state = {
    transaction: undefined,
    interpretResult: undefined,
    currentStep: this.props.step,
    automatic: this.props.automatic,
    inputIndex: this.props.inputIndex,
    transactionId: this.props.transactionId,
    loading: false,
    executingScript: false
  };

  interpretScript = (step) => {
    this.setState({
      ...this.state,
      currentStep: step,
      interpretResult: undefined,
      loading: true
    });

    interpretTransactionInputWithSteps(this.state.transactionId, this.state.inputIndex, step)
      .then((interpretResponse) => {
        this.setState({
          ...this.state,
          loading: false,
          interpretResult: interpretResponse
        });
      })
      .catch((error) => {
        // TODO: handle error
        console.log(error);
      });
  };

  scriptInterpreterWebsocket = new ScriptInterpreterWebsocket();

  interpretScriptWebsocket = () => {
    const initialCallback = () => {
      this.setState({
        ...this.state,
        interpretResult: undefined,
        loading: true,
        executingScript: true
      });
    };

    const closeConnectionCallback = () => {
      this.setState({
        ...this.state,
        executingScript: false
      });
    };

    const onMessageCallback = (interpretResult) => {
      this.setState({
        ...this.state,
        currentStep: interpretResult.step,
        interpretResult: interpretResult,
        loading: false
      });
    };

    const interpreter = this.scriptInterpreterWebsocket.interpreterBuilder(initialCallback, onMessageCallback, closeConnectionCallback);
    interpreter(this.state.transactionId, this.state.inputIndex)
  };


  prevNextButtons = () => {
    const calculatePrevStep = () => {
      return this.state.currentStep > 0 ?
        `/#/transaction/${this.state.transactionId}/input/${this.state.inputIndex}/interpret?step=${parseInt(this.props.step, 10) - 1}` : null;
    };

    const calculateNextStep = () => {
      const result = this.state.interpretResult ? this.state.interpretResult.result.value : false;
      const step = this.props.step ? this.props.step : 0;
      return (result !== true) ?
        `/#/transaction/${this.state.transactionId}/input/${this.state.inputIndex}/interpret?step=${parseInt(step, 10) + 1}` : null;
    };

    if (!this.state.automatic) {
      const prevStep = calculatePrevStep();
      const nextStep = calculateNextStep();

      const disabledIconStyle = {verticalAlign: "middle", fontSize: "16px", color: "grey"};
      const activeIconStyle = {verticalAlign: "middle", fontSize: "16px", color: "rgb(219, 56, 111)"};
      const prevStepClassName = prevStep === null ? "not-active" : "";
      const prevStepIconStyle = prevStep === null ? disabledIconStyle : activeIconStyle;
      const nextStepClassName = nextStep === null ? "not-active" : "";
      const nextStepIconStyle = nextStep === null ? disabledIconStyle : activeIconStyle;

      return (
        <div style={{maxWidth: '550px', textAlign: 'center', margin: '0 auto', marginTop: "30px"}}>
          <Grid container>
            <Grid item sm={6} xs={6} >
              <div>
                <NavigateBeforeIcon style={prevStepIconStyle}/>
                <a className={prevStepClassName} href={prevStep}>
                  <span style={{fontSize: "14px"}}>Prev</span>
                </a>
              </div>
            </Grid>
            <Grid item sm={6} xs={6} >
              <div>
                <a className={nextStepClassName} href={nextStep}>
                  <span style={{fontSize: "14px"}}>Next</span>
                </a>
                <NavigateNextIcon style={nextStepIconStyle}/>
              </div>
            </Grid>
          </Grid>
        </div>
      )
    } else {
      return null;
    }
  };

  interpretState = () => {
    if (this.state.loading) {
      return <Loading/>;
    } else {
      if (this.state.interpretResult) {
        return <InterpreterComponent interpretResult={this.state.interpretResult} step={this.state.currentStep}/>;
      } else {
        return null;
      }
    }
  };

  render() {
    return (
      <div className="container">
        <div className={'application-definition'}>
          <a href={ `/#/transaction/${this.state.transactionId}`}>
            <img src={ mobileLogoImage } className={`logo-image img-responsive mobile`} alt="Bitcoin Playground"/>
          </a>
          <a href={ `/#/transaction/${this.state.transactionId}`}>
            <img src={ desktopLogoImage } className={`logo-image img-responsive desktop`} alt="Bitcoin Playground"/>
          </a>
          {
            this.prevNextButtons()
          }
          {
            this.interpretState()
          }
        </div>
      </div>
    );
  }
}

export default InterpreterContainer;