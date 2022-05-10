import React, { useEffect, useState } from 'react';

import InterpreterComponent from "./InterpreterComponent";
import { interpretTransactionInputWithSteps } from '../../api';
import desktopLogoImage from '../../assets/images/bitcoin-playground-desktop.png';
import mobileLogoImage from '../../assets/images/bitcoin-playground-mobile.png';
import Loading from '../Loading';
import NavigateBeforeIcon from '@material-ui/icons/NavigateBefore';
import NavigateNextIcon from '@material-ui/icons/NavigateNext';
import ScriptInterpreterWebsocket from './ScriptInterpreterWebsocket';
import Grid from "@material-ui/core/Grid/Grid";
import { InterpreterOutcome } from '../../api'

interface InterpreterContainerProps {
    //    children: React.ReactNode,
    transactionId: string,
    inputIndex: number,
    automatic: boolean,
    step: number,
    push: (x: any, y: any) => void
}

interface InterpreterContainerState {
    transaction: string | undefined,
    interpretResult: InterpreterOutcome | undefined,
    currentStep: number,
    automatic: boolean,
    inputIndex: number,
    transactionId: string,
    loading: boolean,
    executingScript: boolean
}

export const InterpreterContainer: React.FunctionComponent<InterpreterContainerProps> = (props) => {

    const [state, setState] = useState<InterpreterContainerState>({
        transaction: undefined,
        interpretResult: undefined,
        currentStep: props.step,
        automatic: props.automatic,
        inputIndex: props.inputIndex,
        transactionId: props.transactionId,
        loading: false,
        executingScript: false
    });

    const scriptInterpreterWebsocket = new ScriptInterpreterWebsocket();

    const interpretScript = (step) => {
        setState({
            ...state,
            loading: true
        });

        interpretTransactionInputWithSteps(state.transactionId, state.inputIndex, step)
            .then((interpretResponse) => {
                setState({
                    ...state,
                    currentStep: step,
                    loading: false,
                    interpretResult: interpretResponse
                });
            })
            .catch((error) => {
                // TODO: handle error
                console.log(error);
            });
    };

    const interpretScriptWebsocket = () => {
        const initialCallback = () => {
            setState({
                ...state,
                loading: true,
                executingScript: true
            });
        };

        const closeConnectionCallback = () => {
            setState(prevState => {
                return {
                    ...prevState,
                    executingScript: false
                }
            });
        };

        const onMessageCallback = (interpretResult) => {
            setState({
                ...state,
                currentStep: interpretResult.step,
                interpretResult: interpretResult,
                loading: false
            });
        };

        const interpreter = scriptInterpreterWebsocket.interpreterBuilder(initialCallback, onMessageCallback, closeConnectionCallback);
        interpreter(state.transactionId, state.inputIndex)
    };

    useEffect(() => {
        if (state.transactionId) {
            if (state.automatic) {
                interpretScriptWebsocket();
            } else {
                const step = props.step ? props.step : 0;
                interpretScript(step);
            }
        }
    }, []);

    useEffect(() => {
        if (!props.automatic && (props.step !== state.currentStep)) {
            interpretScript(props.step);
        }
    }, [props.step]);

    const prevNextButtons = () => {
        const calculatePrevStep = () => {
            return state.currentStep > 0 ?
                `/#/transaction/${state.transactionId}/input/${state.inputIndex}/interpret?step=${props.step - 1}` : undefined;
        };

        const calculateNextStep = () => {
            const result = state.interpretResult ? state.interpretResult.result.value : false;
            const step = props.step ? props.step : 0;
            return (result !== true) ?
                `/#/transaction/${state.transactionId}/input/${state.inputIndex}/interpret?step=${step + 1}` : undefined;
        };

        if (!state.automatic) {
            const prevStep = calculatePrevStep();
            const nextStep = calculateNextStep();

            const disabledIconStyle = { verticalAlign: "middle", fontSize: "16px", color: "grey" };
            const activeIconStyle = { verticalAlign: "middle", fontSize: "16px", color: "rgb(219, 56, 111)" };
            const prevStepClassName = prevStep === undefined ? "not-active" : "";
            const prevStepIconStyle = prevStep === undefined ? disabledIconStyle : activeIconStyle;
            const nextStepClassName = nextStep === undefined ? "not-active" : "";
            const nextStepIconStyle = nextStep === undefined ? disabledIconStyle : activeIconStyle;

            return (
                <div style={{ maxWidth: '550px', textAlign: 'center', margin: '0 auto', marginTop: "30px" }}>
                    <Grid container>
                        <Grid item sm={6} xs={6} >
                            <div>
                                <NavigateBeforeIcon style={prevStepIconStyle} />
                                <a className={prevStepClassName} href={prevStep}>
                                    <span style={{ fontSize: "14px" }}>Prev</span>
                                </a>
                            </div>
                        </Grid>
                        <Grid item sm={6} xs={6} >
                            <div>
                                <a className={nextStepClassName} href={nextStep}>
                                    <span style={{ fontSize: "14px" }}>Next</span>
                                </a>
                                <NavigateNextIcon style={nextStepIconStyle} />
                            </div>
                        </Grid>
                    </Grid>
                </div>
            )
        } else {
            return null;
        }
    };

    const interpretState = () => {
        if (state.loading) {
            return <Loading />;
        } else {
            if (state.interpretResult) {
                return <InterpreterComponent interpretResult={state.interpretResult} step={state.currentStep} />;
            } else {
                return undefined;
            }
        }
    };

    return (
        <div className="container">
            <div className={'application-definition'}>
                <a href={`/#/transaction/${state.transactionId}`}>
                    <img src={mobileLogoImage} className={`logo-image img-responsive mobile`} alt="Bitcoin Playground" />
                </a>
                <a href={`/#/transaction/${state.transactionId}`}>
                    <img src={desktopLogoImage} className={`logo-image img-responsive desktop`} alt="Bitcoin Playground" />
                </a>
                {
                    prevNextButtons()
                }
                {
                    interpretState()
                }
            </div>
        </div>
    );
}

export default InterpreterContainer;