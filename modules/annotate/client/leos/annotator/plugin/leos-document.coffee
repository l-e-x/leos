# Do LEOS specific actions here
$ = require('jquery')
Document = require('../../../src/annotator/plugin/document')

module.exports = class LeosDocument extends Document
  constructor: (element, config) ->
    # Make a definitive check for commentable area Or even better -> read from config
    @containerSelector = "#{config.annotationContainer}"
    @documentIdSelector = "#{config.annotationContainer} #{config.leosDocumentRootNode}"
    super(@getElement(), config)
    
  pluginInit: ->
    @annotator.anchoring = require('../anchoring/leos')
    super

  #LEOS-2789 the reference element 'root' is now defined in the plugin document
  getElement: () ->
    #The root element tag is NOT taken in account while building the xpath by HTML and RANGE classes, 
    #In XPath, we will get sth like '//akomantoso[1]/...'.
    documentContainer = $(@containerSelector)[0]
    return documentContainer

  # LEOS document will provide its own static id
  _getDocumentHref: ->
    leosDocument = $(@documentIdSelector)[0]
    return "uri://LEOS/#{leosDocument.id}" if leosDocument.id
    return super()
