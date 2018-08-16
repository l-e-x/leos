{RangeAnchor, FragmentAnchor, TextPositionAnchor, TextQuoteAnchor} = require('../../../src/annotator/anchoring/types')

domAnchorTextQuote = require('dom-anchor-text-quote')
fragmentAnchor = require('dom-anchor-fragment')

###*
# Converts between TextPositionSelector selectors and Range objects.
###
class LeosAnchor
  constructor: (root, id, exact, prefix, suffix) ->
    @id = id
    @root = root
    @exact = exact
    @prefix = prefix
    @suffix = suffix

  @fromRange: (root, range, options) ->
    fragmentSelector = fragmentAnchor.fromRange(root, range)
    domAnchorQuoteSelector = domAnchorTextQuote.fromRange(_getRootElement(root, fragmentSelector.id), range, options)
    selector = new LeosAnchor(root, fragmentSelector.id, domAnchorQuoteSelector.exact, domAnchorQuoteSelector.prefix, domAnchorQuoteSelector.suffix)
    LeosAnchor.fromSelector(root, selector)

  @fromSelector: (root, selector) ->
    new LeosAnchor(root, selector.id, selector.exact, selector.prefix, selector.suffix)

  toSelector: () ->
    {
      type: 'LeosSelector',
      id: @id,
      exact: @exact,
      prefix: @prefix,
      suffix: @suffix,
    }

  toRange: (options = {}) ->
    range = domAnchorTextQuote.toRange(_getRootElement(@root, @id), this.toSelector(), options)
    if range == null
      throw new Error('Quote not found')
    range

  toPositionAnchor: (options = {}) ->
    anchor = domAnchorTextQuote.toTextPosition(_getRootElement(@root, @id), this.toSelector(), options)
    if anchor == null
      throw new Error('Quote not found')
    new TextPositionAnchor(@root, anchor.start, anchor.end)

  _getRootElement = (root, id) ->
    if !root? and !id?
      throw new Error('Element not found')
    
    try
      return root.querySelector('#' + id)
    catch 
      throw new Error('Element not found')

exports.LeosAnchor = LeosAnchor
exports.FragmentAnchor = FragmentAnchor
exports.RangeAnchor = RangeAnchor
exports.TextPositionAnchor = TextPositionAnchor
exports.TextQuoteAnchor = TextQuoteAnchor