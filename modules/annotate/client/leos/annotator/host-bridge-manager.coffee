
REQUEST_TYPES = ["SecurityToken","UserPermissions","MergeSuggestion"]

module.exports = class HostBridgeManager
  callbackManager: {}

  _addHostBridgeHandlers = (requestType) ->
     self = this
     # Add listeners on cross frame (communication between iframe and host) for each request type
     self.crossframe.on "request#{requestType}", (args..., callback) =>
       console.log("Request #{requestType} to be sent to host")
       if self.hostBridge["request#{requestType}"] and typeof self.hostBridge["request#{requestType}"] == 'function'
         # Add handler on host bridge to let leos application responds
         self.hostBridge["response#{requestType}"] = (data) ->
           console.log("Received message from host for request #{requestType}")
           callback(null, data)
         self.hostBridge["request#{requestType}"](args...)
       else
         callback('No available request handler on bridge')

  constructor: (hostBridge, crossframe) ->
    @crossframe = crossframe
    @hostBridge = hostBridge
    self = this

    if (@crossframe? and @hostBridge?)
      for requestType in REQUEST_TYPES
        _addHostBridgeHandlers.call(self, requestType)
      self.hostBridge.stateChangeHandler = (state) ->
        self.crossframe.call('stateChangeHandler', state)
