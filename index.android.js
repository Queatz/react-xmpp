/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 */
'use strict';
import React, {
	AppRegistry,
	Component,
	DeviceEventEmitter,
	Event,
	NativeModules,
	StyleSheet,
	Subscribable,
	Text,
	TextInput,
	TouchableNativeFeedback,
	View
} from 'react-native';

class XMPPApp extends Component {

	// Setup

	constructor() {
		super();
		
		this.xmpp = NativeModules.ReactXMPP;
		
		this.state = {
			statusText: 'React XMPP Client',
			connected: false,
			sendTo: 'user2',
			sendMessage: '',
			server: '',
			username: 'admin',
			password: "password"
		};
	}
	
	componentWillMount() {
		DeviceEventEmitter.addListener('xmppDisconnect', this.disconnectCallback.bind(this));
		DeviceEventEmitter.addListener('xmppConnect', this.connectCallback.bind(this));
		DeviceEventEmitter.addListener('xmppLoginError', this.loginErrorCallback.bind(this));
		DeviceEventEmitter.addListener('xmppLogin', this.loginCallback.bind(this));
		DeviceEventEmitter.addListener('xmppMessage', this.messageCallback.bind(this));
		DeviceEventEmitter.addListener('xmppMessageError', this.messageErrorCallback.bind(this));
	}
	
	// Callbacks
	
	loginErrorCallback(message) {
		this.setStatus('Login error: ' + message);
	}
	
	messageErrorCallback(message) {
		this.setStatus('Message error: ' + message);
	}
	
	loginCallback() {
		this.setStatus('You are now logged in');
	}
	
	disconnectCallback() {
		this.setStatus('Disconnected');
		this.setState({ connected: false });
	}
	
	connectCallback() {
		this.setStatus('Connected');
		this.setState({ connected: true });
	}
	
	messageCallback(message) {
		this.setStatus(message.from + ' says, "' + message.message + '"');
	}

	setStatus(str) {
		this.setState({ statusText: str });
	}
	
	// Functionality
	
	sendMessage() {
		this.xmpp.message(this.state.sendMessage, this.state.sendTo);
		this.setState({ sendMessage: '' });
	}
	
	login() {
		this.setStatus('Connecting...');
	
		var username = this.state.username;
		var password = this.state.password;
		
		this.xmpp.connect(username, password, this.state.server, 'required');
	}
	
	disconnect() {
		this.xmpp.disconnect();
	}
		
	// Render

	render() {
		return (
			<View style={styles.container}>
			
				<View style={styles.button}>
					<Text>
						 {this.state.statusText}
					</Text>
				</View>
		
				{ !this.state.connected ?
		
				<View style={styles.form}>
				
					<TextInput
						placeholder="Server"
						onChangeText={(text) => this.setState({ server: text })}
						value={this.state.server} />
			
					<TextInput
						placeholder="Username"
						onChangeText={(text) => this.setState({ username: text })}
						value={this.state.username} />
			
					<TextInput
						placeholder="Password"
						onChangeText={(text) => this.setState({ password: text })}
						secureTextEntry={true}
						value={this.state.password} />
		
					<TouchableNativeFeedback onPress={this.login.bind(this)}>
					 	<View style={styles.button}>
						 	<Text style={styles.clickable}>
								Connect
							</Text>
						</View>
					</TouchableNativeFeedback>
					
				</View>
		
				:
				
				<View style={styles.form}>
		
					<TextInput
						placeholder="To User"
						onChangeText={(text) => this.setState({ sendTo: text })}
						value={this.state.sendTo} />
			
					<TextInput
						placeholder="Message"
						onChangeText={(text) => this.setState({ sendMessage: text })}
						value={this.state.sendMessage} />

					<TouchableNativeFeedback onPress={this.sendMessage.bind(this)}>
					 	<View style={styles.button}>
						 	<Text style={styles.clickable}>
								Send
							</Text>
						</View>
					</TouchableNativeFeedback>
				
				</View>
				
				}
				
				{ this.state.connected ?
				
				<TouchableNativeFeedback onPress={this.disconnect.bind(this)}>
				 	<View style={styles.button}>
					 	<Text style={styles.dimmed}>
							Disconnect
						</Text>
					</View>
				</TouchableNativeFeedback>
				
				:
				
				null
				
				}
				
			</View>
		 );
	}
}

// Styles

const styles = StyleSheet.create({
	container: {
		flex: 1,
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: '#d8dadc',
		padding: 12
	},
	form: {
		justifyContent: 'center',
		alignItems: 'center',
		backgroundColor: '#fff',
		padding: 12,
		elevation: 6
	},
	button: {
		padding: 12
	},
	clickable: {	
		color: '#0af'
	},
	dimmed: {
		color: '#aaa',
		margin: 12
	}
});

// App

AppRegistry.registerComponent('XMPPApp', () => XMPPApp);
