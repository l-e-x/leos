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
define(function fragmentTransformerModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LODASH = require("lodash");
    var LOG = require("logger");
    var CKEDITOR = require("promise!ckEditor");
    var attributeTransformerFactory = require("transformer/attributeTransformer");

    /*
     * FragmentTransformer is used to transform arbitrary fragment(tree a like structure of elements) to another fragment. The transformation is done according
     * to the provided transformation config.
     * 
     */
    var fragmentTransformerStamp = STAMPIT()
            .methods(
                    {

                        /*
                         * Used to retrieve transformed fragment(product). @return the transformed fragment
                         * 
                         * @param fragment - required parameter which specifies fragment to be transformed e.g.:
                         * 
                         * From fragment: mp: { attributes: {}, children: [{ text: { value: "text content" } }] }
                         * 
                         * To fragment: p: { attributes: {}, children: [{ text: { value: "text content" } }] }
                         * 
                         * 
                         * @param fragmentConfig - required parameter which specifies how transformation needs to be performed e.g: [ { from: "mp", fromPath:
                         * "mp", to: "p", toPath: "p" },{ from: "text", fromPath: "mp/text", to: "text", toPath: "p/text" } ]
                         * 
                         * 
                         */
                        getTransformedElement : function getTransformedElement(params) {
                            this._validateRequired([ "fragment", "transformationConfigResolver", "direction" ], params);
                            this._init(params);
                            var product = null;
                            var outerLevelTransformationContext = this._.fragment.transformationContext;
                            if (this._.fragmentConfig) {
                                this._.transformationConfigResolver = params.transformationConfigResolver;
                                var binded_createProductsForElement = LODASH.bind(this._createProductsForElement, this);
                                if (params.fragment instanceof CKEDITOR.htmlParser.text) {
                                    this._createProductsForElement(params.fragment);
                                } else {
                                    params.fragment.forEach(binded_createProductsForElement);
                                }
                                product = this._getTopFragmentProductForElement(params.fragment);
                            } else {
                                if (this._.fragment.name && this._.fragment.name !== 'body') {
                                    // not able to transform element according to global configs, skipping and warning, read on: LEOS-1342
                                    var warningMessage = [ "Fragment: ", this._getElementAsString(this._.fragment),
                                            " is not recognized and is pass through as it is." ];
                                    LOG.warn(warningMessage.join(""));
                                }

                            }

                            if (product && this._.fragmentConfig.transformer) {
                                this._.product = product;
                                var currentTransformer = this._.fragmentConfig.transformer[this._.direction].action.bind(this);
                                var that = this;
                                this._.fragment.forEach(function(element) {
                                    if (!that._isTopLevelElement(element)) {
                                        element.transformationContext = that._getTransformationContext(element);
                                    }
                                    currentTransformer(element);
                                    if (element.transformationContext.skipChildren) {
                                        // return false so that forEach iterator won't iterate over its children
                                        return false;
                                    }
                                });

                            }

                            this._.fragment.transformationContext = outerLevelTransformationContext;
                            return product;
                        },
                        _init : function _init(params) {
                            this._ = {};
                            this._.direction = params.direction;
                            this._.fragment = params.fragment;
                            this._.orderOfChildrensForParent = {};
                            this._.fragmentConfig = params.transformationConfigResolver.getConfig(params.fragment, this._.direction);
                        },

                        /*
                         * Retrieve the product with the shortest path for given element @param element - under transformation
                         * 
                         */
                        _getTopFragmentProductForElement : function _getTopFragmentProductForElement(element) {
                            var pathToProduct = element.transformationContext.pathToProduct;
                            var paths = LODASH.keys(pathToProduct);
                            var product = null;
                            if (paths.length !== 0) {
                                var topProductPath = LODASH.reduce(paths, function getShorterStringForTwo(a, b) {
                                    return a.length < b.length ? a : b;
                                });
                                var pathToProductConfig = this._pullProductFromPathToProduct({
                                    element : element,
                                    path : topProductPath
                                });
                                if (pathToProductConfig) {
                                    product = pathToProductConfig.product;
                                }
                                // pathToProduct[topProductPath].product;
                            }
                            return product;
                        },

                        /*
                         * This method prepares element specific data for transformation i.e.: element name, path of the element in the fragment, element
                         * configuration object which denotes to what product element needs to be transformed One element config describe transformation of the
                         * element to one product element. Element can have more than one of such transformation config because of that from one element there
                         * can be produced more than one products @param element - element being processed
                         */
                        _createProductsForElement : function _createProductsForElement(element) {
                            var transformationContext = this._getTransformationContext(element);
                            element.transformationContext = transformationContext;
                            if (transformationContext.elementConfigs.length === 0) {
                                if (this._.fragmentConfig.transformer) {
                                    return false;
                                }
                                this._handleNonExistedElementConfig(element);
                            } else {
                                this._createProductsForElementWithContext(element);
                            }

                            if (element.transformationContext.skipChildren) {
                                // return false so that forEach iterator won't iterate over its children
                                return false;
                            }
                            // return true so that forEach iterator iterate over its children
                            return true;

                        },
                        _handleNonExistedElementConfig : function _handleNonExistedElementConfig(element) {
                            // if no existed element config set flag skip children to true
                            element.transformationContext.skipChildren = true;
                            // not able to transform element according to current config
                            if (this._isWhitespaceElement(element)) {
                                // white spaces detected, skipping them
                                return;
                            } else {
                                var globalConfig = this._.transformationConfigResolver.getConfig(element, this._.direction);
                                if (!globalConfig) {
                                    // not able to transform element according to global configs, skipping and warning
                                    var warningMessage = [ "Element: ", this._getElementAsString(element), " with path: ",
                                            element.transformationContext.elementPath, " is not recognized under fragment: ",
                                            this._getElementAsString(this._.fragment), " and is skipped." ];
                                    LOG.warn(warningMessage.join(""));
                                } else {
                                    var product = this._getNestedProduct(element);
                                    if (product) {
                                        var productParent = this._getTheMostNestedProductForElementParent(element);
                                        if (productParent) {
                                            productParent.add(product);
                                        }

                                    }
                                }
                            }
                        },
                        _isWhitespaceElement : function _isWhitespaceElement(element) {
                            return element.transformationContext.elementName === "text" && element.value.trim() === "";
                        },
                        _getTransformationContext : function _getTransformationContext(element) {
                            var elementName = this._getElementName(element);
                            var elementPath = this._getElementPath(element);
                            var transformationContext = {
                                elementName : elementName,
                                elementPath : elementPath,
                                pathToProduct : {}
                            };
                            transformationContext.elementConfigs = this._getElementConfigsForPath(elementPath);
                            return transformationContext;
                        },
                        /*
                         * Utility method to get simplified string representation of the element
                         */
                        _getElementAsString : function _getElementAsString(element) {
                            var elementAsString = [ "<", this._getElementName(element) ];
                            LODASH.forEach(element.attributes, function(value, key) {
                                elementAsString = elementAsString.concat([ ' ', key, '="', value, '"' ]);
                            });
                            elementAsString.push(">")
                            return elementAsString.join("");
                        },

                        /*
                         * 
                         * This method is responsible for creating the products corresponding to the current element being processed. Any element in the
                         * fragment being transformed can be transformed to one or more products For every product parent-child relation is set. @param element -
                         * element being processed
                         */
                        _createProductsForElementWithContext : function _createProductsForElementWithContext(element) {
                            var elementConfigs = element.transformationContext.elementConfigs;
                            var product;
                            // check if current element being transformed has any transformations configs
                            if (elementConfigs && elementConfigs.length !== 0) {
                                for (var ii = 0, length = elementConfigs.length; ii < length; ii++) {
                                    var elementConfig = elementConfigs[ii];
                                    product = this._createProduct(element, elementConfig);

                                    this._moveAttrs(element, elementConfig, product);
                                }
                            }

                        },
                        _moveAttrs : function(element, elementConfig, product) {
                            if (elementConfig.attrs) {
                                for (var ii = 0, length = elementConfig.attrs.length; ii < length; ii++) {
                                    var attrConfig = elementConfig.attrs[ii];
                                    if (attrConfig.action) {
                                        var attributeTransformer = attributeTransformerFactory.getAttributeTransformer(attrConfig.action);
                                        attributeTransformer.perform.call({
                                            toElement : product,
                                            fromElement : element,
                                            attrConfig : attrConfig
                                        });
                                    }
                                }
                            }
                        },

                        /*
                         * This method creates new product for element being processed or reuse existing one for the element's path. The new product is created
                         * when the value 'toPath' is requested for the first time. All the subsequent calls with the same value of the 'toPath' merely reuse
                         * already existed product. This allows to map one element to multiple products. @param element - element being processed @param
                         * elementConfig - transformation object which describe how the elements needs to be transformed @return product - the result of the
                         * element's transformation with given elementConfig
                         * 
                         * 
                         */
                        _createProduct : function _createProduct(element, elementConfig) {
                            var product = this._getProductForPath(elementConfig.toPath, element);
                            if (!product) {
                                product = this._crateElementForName(elementConfig.to);
                                this._setParentChildRelationForProduct(element, elementConfig, product);
                            }
                            this._pushProductToPathAndProductHash({
                                element : element,
                                product : product,
                                elementConfig : elementConfig
                            });
                            this._moveValueForTextNode(element, elementConfig, product);
                            return product;
                        },
                        mapToProducts : function mapToProducts(element, elementConfigs) {
                            element.transformationContext = element.transformationContext || this._getTransformationContext(element);
                            element.transformationContext.elementConfigs = this._normalizeElementConfigs(elementConfigs);
                            this._createProductsForElementWithContext(element);
                        },
                        _pushProductToPathAndProductHash : function _pushProductToPathAndProductHash(pathToProductConfig) {
                            var pathToProduct = pathToProductConfig.element.transformationContext.pathToProduct;
                            if (!pathToProduct[pathToProductConfig.elementConfig.toPath]) {
                                pathToProduct[pathToProductConfig.elementConfig.toPath] = [];
                            }
                            pathToProduct[pathToProductConfig.elementConfig.toPath].push({
                                product : pathToProductConfig.product,
                                config : pathToProductConfig.elementConfig
                            });
                        },
                        _pullProductFromPathToProduct : function _pullProductFromPathToProduct(pathToProductConfig) {
                            var productsForPaths = pathToProductConfig.element.transformationContext.pathToProduct[pathToProductConfig.path];
                            var recentProductForPath;
                            if (productsForPaths) {
                                recentProductForPath = productsForPaths[productsForPaths.length - 1];
                            }
                            return recentProductForPath;
                        },

                        _appendChildToProduct : function _appendChildToProduct(element, elementConfig) {
                            if (!elementConfig.toChildProduct && !elementConfig.toChild) {
                                throw new Error("At least one of the following properties: 'toChildProduct' or 'toChild' needs to be provided.")
                            }
                            if (!elementConfig.toChildProduct) {
                                elementConfig.toChildProduct = this._crateElementForName(elementConfig.toChild);
                                if (elementConfig.toChild === "text") {
                                    elementConfig.toChildProduct.value = elementConfig.toChildTextValue;
                                }
                            }

                            var parentProduct = this._getProductForPath(elementConfig.toPath, element);
                            if (!parentProduct) {
                                LOG.error("Element with name: ", elementConfig.toChild, " created. But can not be attached to parent.",
                                        "Parent element with path: ", elementConfig.toPath, " does not exists.");
                            } else {
                                parentProduct.add(elementConfig.toChildProduct);
                                this._pushProductToPathAndProductHash({
                                    element : element,
                                    product : elementConfig.toChildProduct,
                                    elementConfig : {
                                        toPath : (elementConfig.toPath + "/" + elementConfig.toChild)
                                    }
                                });
                                this._moveAttrs(element, elementConfig, elementConfig.toChildProduct);
                            }

                        },
                        mapToChildProducts : function mapToChildProducts(element, elementConfigs) {
                            element.transformationContext = element.transformationContext || this._getTransformationContext(element);
                            element.transformationContext.elementConfigs = this._normalizeElementConfigs(elementConfigs);
                            var that = this;
                            element.transformationContext.elementConfigs.forEach(function(config) {
                                that._appendChildToProduct(element, config);
                            })
                        },
                        mapToNestedChildProduct : function mapToNestedChildProduct(element, elementConfig) {
                            element.transformationContext = element.transformationContext || this._getTransformationContext(element);
                            var nestedProduct = this._getNestedProduct(element);
                            if (nestedProduct) {
                                this._appendChildToProduct(element.parent, {
                                    toPath : elementConfig.toPath,
                                    toChildProduct : nestedProduct
                                });
                            }
                        },
                        _normalizeElementConfigs : function _normalizeElementConfigs(elementConfigs) {
                            var configs = elementConfigs.constructor !== Array ? [ elementConfigs ] : elementConfigs;
                            configs.forEach(function(config) {
                                if (!config.toPath) {
                                    throw new Error("Missing required 'toPath' attribute.");
                                } else {
                                    if (!config.to) {
                                        var pathParts = config.toPath.split("/");
                                        if (!config.to) {
                                            config.to = pathParts[pathParts.length - 1];
                                        }

                                        if (!config.toParentPath) {
                                            var parentPathLength = (pathParts.length > 1) ? pathParts.length - 1 : 1
                                            config.toParentPath = pathParts.slice(0, parentPathLength).join("/");
                                        }
                                    }
                                }

                            })
                            return configs;

                        },
                        _getNestedProduct : function _getNestedProduct(element) {
                            if (element.isNested === true) {
                                // don't do nested transformation when already tried one
                                return;
                            } else {
                                element.isNested = true;
                            }
                            var fragmentTransformer = fragmentTransformerStamp();
                            var product = fragmentTransformer.getTransformedElement({
                                fragment : element,
                                direction : this._.direction,
                                transformationConfigResolver : this._.transformationConfigResolver
                            });
                            element.transformationContext.skipChildren = true;
                            return product;
                        },
                        _getTheMostNestedProductForElementParent : function _getTheMostNestedProductForElementParent(element) {
                            var pathToProduct = element.parent.transformationContext.pathToProduct;
                            var that = this;
                            var mostNestedProductPath = LODASH.maxBy(LODASH.keys(pathToProduct), function(productPath) {
                                var configElementWithProduct = that._pullProductFromPathToProduct({
                                    element : element.parent,
                                    path : productPath
                                });
                                if (productPath.match(/text$/)) {
                                    return -1;
                                }
                                if (configElementWithProduct && configElementWithProduct.config.noNestedAllowed) {
                                    return -1;
                                }

                                return productPath.length;
                            });
                            var mostNestedProduct = this._pullProductFromPathToProduct({
                                element : element.parent,
                                path : mostNestedProductPath
                            }).product;
                            ;
                            return mostNestedProduct;
                        },

                        /*
                         * Sets the child-parent relation for new product @param element - element being processed @param elementConfig - transformation object
                         * which describe how the elements needs to be transformed @param product - the result of the element's transformation with given
                         * elementConfig
                         * 
                         */
                        _setParentChildRelationForProduct : function _setParentChildRelationForProduct(element, elementConfig, product) {
                            var parentProduct = this._getProductForPath(elementConfig.toParentPath, element);
                            if (parentProduct && parentProduct !== product) {
                                if (parentProduct.children.length === 0) {
                                    parentProduct.add(product);
                                } else {
                                    var insertionIndex;
                                    var configsForParentChildren = this._getChildrenOrderForParent(elementConfig.toParentPath);
                                    parentProduct.children.forEach(function(element, index, array) {
                                        if (configsForParentChildren[product.name] < configsForParentChildren[element.name]) {
                                            insertionIndex = index;
                                            return;
                                        } else if (array.length - 1 === index) {
                                            insertionIndex = index + 1;
                                        }
                                    });
                                    parentProduct.add(product, insertionIndex);
                                }

                            }

                        },
                        /*
                         * For given 'toParentPath' the order of the product children is return in which they should be placed in the parent product
                         */
                        _getChildrenOrderForParent : function _getChildrenOrderForParent(toParentPath) {
                            var orderOfChildrensForParent = {};
                            if (this._.orderOfChildrensForParent[toParentPath]) {
                                return this._.orderOfChildrensForParent[toParentPath];
                            }
                            this._.orderOfChildrensForParent[toParentPath] = {};
                            var order = 0, that = this;
                            this._.fragmentConfig.forEach(function(config) {
                                if (toParentPath === config.toParentPath && config.toParentPath !== config.toPath) {
                                    that._.orderOfChildrensForParent[toParentPath][config.to] = order;
                                    order += 1;
                                }
                            });
                            return this._.orderOfChildrensForParent[toParentPath];
                        },
                        /*
                         * This method cover following use cases for text node values: 1. move value from text node to attribute of non text node 2. the
                         * opposite of the first i.e.: move attribute of non text node to text node 3. move text node value to the text node value @param
                         * element - element being processed @param elementConfig - transformation object which describe how the element needs to be transformed
                         * @param product - product - the result of the element's transformation with given elementConfig
                         * 
                         */
                        _moveValueForTextNode : function _moveValueForTextNode(element, elementConfig, product) {
                            // only execute if the element or product is text node
                            if (elementConfig.to === "text" || element.transformationContext.elementName === "text") {
                                var elementValue = null;
                                if (element.transformationContext.elementName === "text") {
                                    elementValue = element.value;
                                } else {
                                    elementValue = element.attributes[elementConfig.fromAttribute.toLowerCase()];
                                }

                                if (elementConfig.to === "text") {
                                    product.value = elementValue;
                                } else {
                                    if (elementConfig.toAttribute) {
                                        product.attributes[elementConfig.toAttribute] = elementValue;
                                    }
                                }
                            }
                        },
                        /*
                         * Return the product associated with given path. Only one product can be associated with given path @param path of element in the
                         * fragment @param element being processed @return existing product for the element if any
                         */
                        _getProductForPath : function _getProductForPath(path, element) {
                            var productForPath;
                            var product;
                            var currentElement = element;
                            do {
                                productForPath = this._pullProductFromPathToProduct({
                                    element : currentElement,
                                    path : path
                                });
                                if (productForPath) {
                                    product = productForPath.product;
                                    break;
                                }

                                if (this._isTopLevelElement(currentElement)) {
                                    break;
                                }

                                currentElement = currentElement.parent;
                            } while (currentElement && currentElement.transformationContext);
                            return product;
                        },
                        /*
                         * Creates the ckEditor element for given name @param name - element's name @return the ckEditor element
                         */
                        _crateElementForName : function _crateElementForName(name) {
                            var product;
                            if (name === "text") {
                                product = new CKEDITOR.htmlParser.text();
                            } else {
                                product = new CKEDITOR.htmlParser.element(name);
                            }
                            return product;
                        },

                        /*
                         * Return name for ckEditor element. @param element - ckEditor element @return element's name
                         * 
                         */
                        _getElementName : function _getElementName(element) {
                            var elementName = null;
                            if (element instanceof CKEDITOR.htmlParser.element) {
                                elementName = element.name;
                            } else if (element instanceof CKEDITOR.htmlParser.text) {
                                elementName = "text";

                            }
                            return elementName;
                        },
                        /*
                         * Return the path of the element in the fragment. @param element - ckEditor element @return path of the element in the fragment
                         * 
                         */
                        _getElementPath : function _getElementPath(element) {
                            var partsOfFragmentPath = [];
                            var currentElement = element;
                            do {
                                partsOfFragmentPath.unshift(this._getElementName(element).toLowerCase());
                                if (this._isTopLevelElement(currentElement)) {
                                    break;
                                } else {
                                    currentElement = currentElement.parent;
                                }
                                // check if the path is calculated for current element's parent if yes use it
                                if (currentElement.partsOfFragmentPath) {
                                    partsOfFragmentPath = currentElement.partsOfFragmentPath.concat(partsOfFragmentPath);
                                    break;
                                }
                            } while (!this._isTopLevelElement(currentElement));
                            element.partsOfFragmentPath = partsOfFragmentPath;
                            return partsOfFragmentPath.join("/");
                        },
                        /*
                         * Return all transformation configs associated with the path @param path provides nesting info of element inside fragment @return the
                         * transformation configs for path
                         */
                        _getElementConfigsForPath : function _getElementConfigsForPath(path) {
                            var elementConfigs = [];
                            for (var ii = 0, length = this._.fragmentConfig.length; ii < length; ii++) {
                                if (this._.fragmentConfig[ii].fromPath == path) {
                                    elementConfigs.push(this._.fragmentConfig[ii]);
                                }
                            }
                            return elementConfigs;
                        },
                        _validateRequired : function _validateRequired(requiredParams, params) {
                            requiredParams.forEach(function(paramName) {
                                if (!params || !params[paramName]) {
                                    var errorMessage = [ "Param with name: '", paramName, "' is required, please provide it." ].join("");
                                    throw new Error(errorMessage);
                                }
                            });
                        },
                        _isTopLevelElement: function _isTopLevelElement(element) {
                            return element === this._.fragment;
                        }

                    });

    return fragmentTransformerStamp;
});