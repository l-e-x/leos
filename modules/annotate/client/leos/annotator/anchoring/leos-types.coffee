{RangeAnchor, FragmentAnchor, TextPositionAnchor, TextQuoteAnchor} = require('../../../src/annotator/anchoring/types')

domAnchorTextQuote = require('dom-anchor-text-quote')
domAnchorTextPosition = require('dom-anchor-text-position')
fragmentAnchor = require('dom-anchor-fragment')

###*
# Converts between TextPositionSelector selectors and Range objects.
###
class LeosAnchor
  constructor: (root, id, exact, prefix, suffix, start, end) ->
    @id = id
    @root = root
    @exact = exact
    @prefix = prefix
    @suffix = suffix
    @start = start
    @end = end

  @fromRange: (root, range, options) ->
    fragmentSelector = fragmentAnchor.fromRange(root, range)
    domAnchorQuoteSelector = domAnchorTextQuote.fromRange(_getRootElement(root, fragmentSelector.id), range, options)
    domAnchorPositionSelector = domAnchorTextPosition.fromRange(_getRootElement(root, fragmentSelector.id), range, options)
    selector = new LeosAnchor(root, fragmentSelector.id, domAnchorQuoteSelector.exact, domAnchorQuoteSelector.prefix, domAnchorQuoteSelector.suffix, domAnchorPositionSelector.start, domAnchorPositionSelector.end)
    LeosAnchor.fromSelector(root, selector)

  @fromSelector: (root, selector) ->
    new LeosAnchor(root, selector.id, selector.exact, selector.prefix, selector.suffix, selector.start, selector.end)

  toSelector: () ->
    {
      type: 'LeosSelector',
      id: @id,
      exact: @exact,
      prefix: @prefix,
      suffix: @suffix,
      start: @start,
      end: @end
    }

  toRange: (options = {}) ->
    try
      range = domAnchorTextQuote.toRange(_getRootElement(@root, @id), this.toSelector(), options)
      if range == null
        range = domAnchorTextPosition.toRange(_getRootElement(@root, @id), this.toSelector(), options)
    catch error
      if (error.message.indexOf("Failed to execute 'setEnd' on 'Range'") != -1)
        if !@start? or !@end? or @start == @end == 0
          throw new Error('Range creation failed')
        @end -= 1
        range = domAnchorTextPosition.toRange(_getRootElement(@root, @id), this.toSelector(), options)
    if range == null
      throw new Error('Range creation failed')
    range

  toPositionAnchor: (options = {}) ->
    positionAnchorRoot = _getRootElement(@root, @id)
    anchor = domAnchorTextQuote.toTextPosition(positionAnchorRoot, this.toSelector(), options)
    if anchor == null
      throw new Error('Quote not found')
    new TextPositionAnchor(positionAnchorRoot, anchor.start, anchor.end)

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