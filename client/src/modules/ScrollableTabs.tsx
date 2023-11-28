import React from 'react';
import _ from 'lodash';
import PropTypes from 'prop-types';
import { makeStyles } from 'tss-react/mui';
import Tabs from '@mui/material/Tabs';
import Tab from '@mui/material/Tab';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';

function TabPanel(props) {
    const { children, value, index, ...other } = props;

    return (
        <Typography
            component="div"
            role="tabpanel"
            hidden={value !== index}
            id={`scrollable-auto-tabpanel-${index}`}
            aria-labelledby={`scrollable-auto-tab-${index}`}
            {...other}
        >
            {value === index && <Box p={0}>{children}</Box>}
        </Typography>
    );
}

TabPanel.propTypes = {
    children: PropTypes.node,
    index: PropTypes.any.isRequired,
    value: PropTypes.any.isRequired,
};

function a11yProps(index: string) {
    return {
        id: `scrollable-auto-tab-${index}`,
        'aria-controls': `scrollable-auto-tabpanel-${index}`,
    };
}

const useStyles = makeStyles()(theme => ({
    root: {
        flexGrow: 1,
        width: '100%',
        backgroundColor: theme.palette.background.paper,
    },
}));

export default function ScrollableTabs(props) {
    //const { title1, children1, title2, children2, ...other } = props;
    const { tabs } = props;
    const { classes } = useStyles();
    const [value, setValue] = React.useState(0);

    const handleChange = (event, newValue) => {
        setValue(newValue);
    };

    return (
        <div className={classes.root}>
            <Tabs
                value={value}
                onChange={handleChange}
                indicatorColor="primary"
                textColor="primary"
                variant="fullWidth"
                scrollButtons="auto"
                aria-label="scrollable auto tabs example"
                centered
            >
                {
                    _.map(tabs, (tab, index) => {
                        return <Tab label={tab.title} {...a11yProps(index)} key={index} />;
                    })
                }
            </Tabs>
            {
                _.map(tabs, (tab, index) => {
                    return (
                        <TabPanel value={value} index={index} key={index}>
                            {tab.children}
                        </TabPanel>
                    );
                })
            }
        </div>
    );
}