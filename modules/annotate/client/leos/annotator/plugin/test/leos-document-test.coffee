$ = require('jquery')
LeosDocument = require('../leos-document')

describe 'LeosDocument', ->
  testDocument = null

  beforeEach ->
    testDocument = new LeosDocument($(document)[0], {annotationContainer: '#docContainer', leosDocumentRootNode: 'akomantoso'})
    body = $("body")
    body.append('<div id="docContainer"><akomantoso id="docId"></akomantoso></div>')

  afterEach ->
    $(document).unbind()

  describe 'leos document contructor', ->
    it 'should have id', ->
      assert.equal(testDocument._getDocumentHref(), "uri://LEOS/docId")

    it 'should have element', ->
      assert.equal(testDocument.getElement(), $("#docContainer")[0])

  describe 'leos document refresh', ->
    $( "#docContainer" ).remove();
    body = $("body")
    body.append('<div id="docContainer"><akomantoso id="docId"></akomantoso></div>')

    it 'should have id', ->
      assert.equal(testDocument._getDocumentHref(), "uri://LEOS/docId")

    it 'should have element', ->
      assert.equal(testDocument.getElement(), $("#docContainer")[0])
