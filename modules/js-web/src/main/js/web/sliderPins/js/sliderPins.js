/*
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
; // jshint ignore:line
(function(root, factory) {
    if (typeof define === 'function' && define.amd) {
        define(['jquery'], factory);
    } else if (typeof exports === 'object') {
        // Node, CommonJS-like
        module.exports = factory(require('jquery'));
    } else {
        // Browser globals (root is window)
        root.returnExports = factory(root.jQuery);
    }
}(this, function sliderMarkerFactory($) {
    'use strict';

    var NAVIGATION = {
        NEXT : "NEXT",
        PREV : "PREV"
    }
    
    /* Utility Functions*/
    function _addClass(el, className) {
        if ( el.classList ) {
            el.classList.add(className);
        } else {
            el.className += ' ' + className;
        }
    }

    function _roundOffTo(floatNumber, digitsAfterDecimal) {
        return floatNumber.toFixed(digitsAfterDecimal);
    }

    /* Main Function*/
    function SliderPins(target, selectorStyleMap) {

        // Create the marker Container element, initialise HTML and set classes.
        var _pinContainer = _createPinContainer();
        _attachTo(target.parentNode, _pinContainer);

        var _pins = _addPins(target, _pinContainer, selectorStyleMap);

        return {
            pins: _pins,
            pinContainer: _pinContainer,
            target: target
        };

        /* private functions */
        //Create a container to place pins
        function _createPinContainer() {
            var div = document.createElement('div');
            _addClass(div, "pin-container");
            _addClass(div, "pin-right");
            return div;
        }

        function _attachTo(parent,child) {
            parent.appendChild(child);
        }

        //adds pins for all elements found for passes selectors-styles in map
        function _addPins(target, pinContainer, map) {
            var pins = [];
            Object.keys(map).forEach(_findAndCreate);
            return pins;

            function _findAndCreate(selector) {
                $(target).find(selector).each(function _createAndAttachPin(elIndex, el) {

                    var pinElement =_createPin(pins.length + 1, el, map[selector], target);
                    if (_uniquePin(pins, pinElement)) {
                        _attachTo(pinContainer, pinElement);
                        pins.push(pinElement);
                    }
                });
            }
        }

        function _uniquePin(pins, el){
            var _top = el.style.top;
            var _class = el.className;
            return pins.every(function checkPin(pin){
               return (_top !== pin.style.top || _class !== pin.className);
            });
        }

        function _createPin(index, refToElement, pinSytle, target) {
            //create marker
            var pinDiv = document.createElement('div');
            _addClass(pinDiv, "pin");
            _addClass(pinDiv, pinSytle);

            pinDiv.setAttribute("data-index", index);
            pinDiv.style.top = _getPercentageDistanceFromTop(refToElement, target);
            pinDiv.refTo = refToElement;
            pinDiv.addEventListener("click", _scrollTargetPane);

            //pinDiv.setAttribute("title", refToElement.textContent);
            return pinDiv;

            function _scrollTargetPane() {
                var element = this.refTo;
                _scrollToChange(element, target);
            }
        }

        function _getPercentageDistanceFromTop(element, target) {
            //find total height of target
            var total_height = target.scrollHeight;
            //find referenced element position from top
            var element_pos_from_target_top = _getElementDistanceFromTop(element,target);
            //get percentage to one decimal
            return _roundOffTo((100 * element_pos_from_target_top / total_height), 2) + "%";
        }
    }

    // Run the standard initializer
    function _initialize(targetId, selectorStyleMap) {
        var target = document.getElementById(targetId);
        _checkPreConditions(target);
        // create the slider environment;
        target.SliderPins = SliderPins(target, selectorStyleMap);
        return target.SliderPins;
    }

    /* public functions*/
    function _destroy(targetId) {
        var target = document.getElementById(targetId);
        if (target && target.SliderPins) {
            target.parentNode.removeChild(target.SliderPins.pinContainer);
            delete target.SliderPins;
        }
    }

    function _checkPreConditions(target) {
        if (!target.nodeName) {
            throw new Error('SliderPins requires a single element to apply the Pins!!');
        }
        var position = window.getComputedStyle(target.parentElement).getPropertyValue("position");
        if (position !== 'absolute' && position !== 'relative') {
            console.log("Slider pins may not be placed correctly if 'position' style of Target.parentNode is other than relative or absolute:" + position);
        }
        // Throw an error if the slider was already initialized.
        if (target.SliderPins) {
            throw new Error('SliderPIns was already initialized.');
        }
    }

    function _scrollToChange(element, target) {
        var newScrollTop = _getElementDistanceFromTop(element, target) - 76; //76 is random factor to show element just a little below
        $(target).animate({scrollTop: newScrollTop}, 500, "swing");
    }
    
    function _getElementDistanceFromTop(element, target) {
        element = _getPositionedElement(element);// this fix is required for the elements which are not displayed
        //get bounding rect returns position corresponding to browser top. So subtracting container top to convert it to reference to container
        var currentElementPositionRelativeToScreen = element.getBoundingClientRect().top - target.getBoundingClientRect().top;
        //adding already scrolled factor to position to make it relative to target
        var distanceFromTargetTop = target.scrollTop + currentElementPositionRelativeToScreen;

        return distanceFromTargetTop;
    }
    
    function _getPositionedElement(element){
        while($(element).is(':hidden')) {
            element = element.parentElement;
        }
        return element;
    }

    function _navigate(targetId, direction) {
        var target = document.getElementById(targetId);
        var _pins = target.SliderPins.pins;
        _pins = _pins.sort(_sortByTop);
        _pins = direction === NAVIGATION.PREV ? _pins.reverse() : _pins;    //reversing the pins array in case of previous to traverse pins from bottom to top

        _pins.some(function(pin, index, _pins) {
             var changedElement = pin.refTo;
             if(checkElementInViewport(changedElement, target, direction)) {
                 _scrollToChange(changedElement, target);
                 return true;
             }
        });
    }
    
    function _sortByTop(pin1, pin2) {
        var top1 = _parse(pin1.style.top), top2 = _parse(pin2.style.top);
        return top1 <= top2 ? -1 : 1;
    }
    
    function _parse(position) {
        position = position.slice(0, position.length - 1);
        position = parseFloat(position);
        return position;
    }
    
    function checkElementInViewport(element,target, direction) {
        var elementTop = element.getBoundingClientRect().top,
        targetTop = target.getBoundingClientRect().top;
       
        return direction === NAVIGATION.NEXT ? elementTop > (targetTop + 77) : elementTop < (targetTop + 75);   //77 & 75 are offsets added based on the scrollToChange() method which adds an offset of 76.
                                                                                                                //These are set one more or less depending on the next or previous change so that it will be slightly
                                                                                                                //above or below the current viewing area (i.e. 76).
    }

    return {
        create: _initialize,
        destroy: _destroy,
        navigate: _navigate
    };


}));