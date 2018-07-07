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
define(function transformationsConfigUtilModule(require) {
    "use strict";

    var STAMPIT = require("stampit");
    var LOG = require("logger");
    var ConfigNormalizerStamp = require("transformer/configNormalizer");
    var LODASH = require("lodash");

    var transformationsConfigUtilStamp = STAMPIT().enclose(function init() {
        this._ = {};
        this.configs = {
            aknAlinea: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [{
                            "to": "data-akn-name",
                            "toValue": "alinea",
                            "action": "addAttributeTransformer"
                        }],
                        "fromPath": "alinea",
                        "fromParentPath": "alinea",
                        "from": "alinea",
                        "toPath": "p",
                        "toParentPath": "p",
                        "to": "p"
                    }, {
                        "attrs": [],
                        "fromPath": "alinea/content",
                        "fromParentPath": "alinea",
                        "from": "content",
                        "toPath": "p",
                        "toParentPath": "p",
                        "to": "p"
                    }, {
                        "attrs": [],
                        "fromPath": "alinea/content/mp",
                        "fromParentPath": "alinea/content",
                        "from": "mp",
                        "toPath": "p",
                        "toParentPath": "p",
                        "to": "p"
                    }, {
                        "attrs": [],
                        "fromPath": "alinea/content/mp/text",
                        "fromParentPath": "alinea/content/mp",
                        "from": "text",
                        "toPath": "p/text",
                        "toParentPath": "p",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [{
                            "from": "data-akn-name",
                            "fromValue": "alinea"
                        }],
                        "fromPath": "p",
                        "fromParentPath": "p",
                        "from": "p",
                        "toPath": "alinea",
                        "toParentPath": "alinea",
                        "to": "alinea"
                    }, {
                        "attrs": [],
                        "fromPath": "p",
                        "fromParentPath": "p",
                        "from": "p",
                        "toPath": "alinea/content",
                        "toParentPath": "alinea",
                        "to": "content"
                    }, {
                        "attrs": [],
                        "fromPath": "p",
                        "fromParentPath": "p",
                        "from": "p",
                        "toPath": "alinea/content/mp",
                        "toParentPath": "alinea/content",
                        "to": "mp"
                    }, {
                        "attrs": [],
                        "fromPath": "p/text",
                        "fromParentPath": "p",
                        "from": "text",
                        "toPath": "alinea/content/mp/text",
                        "toParentPath": "alinea/content/mp",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: "alinea",
                    html: 'p',
                    attr: [{
                        html: ["data-akn-name", "alinea"].join("=")
                    }],
                    sub: {
                        akn: "content",
                        html: "p",
                        sub: {
                            akn: "mp",
                            html: "p",
                            sub: {
                                akn: "text",
                                html: "p/text"
                            }
                        }
                    }
                }
            },
            aknHtmlItalic: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [],
                        "fromPath": "i",
                        "fromParentPath": "i",
                        "from": "i",
                        "toPath": "em",
                        "toParentPath": "em",
                        "to": "em",
                    }, {
                        "attrs": [],
                        "fromPath": "i/text",
                        "fromParentPath": "i",
                        "from": "text",
                        "toPath": "em/text",
                        "toParentPath": "em",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [],
                        "fromPath": "em",
                        "fromParentPath": "em",
                        "from": "em",
                        "toPath": "i",
                        "toParentPath": "i",
                        "to": "i",
                    }, {
                        "attrs": [],
                        "fromPath": "em/text",
                        "fromParentPath": "em",
                        "from": "text",
                        "toPath": "i/text",
                        "toParentPath": "i",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: "i",
                    html: "em",
                    sub: {
                        akn: "text",
                        html: "em/text"
                    },
                }
            },

            aknHtmlUnderline: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [],
                        "fromPath": "u",
                        "fromParentPath": "u",
                        "from": "u",
                        "toPath": "u",
                        "toParentPath": "u",
                        "to": "u",
                    }, {
                        "attrs": [],
                        "fromPath": "u/text",
                        "fromParentPath": "u",
                        "from": "text",
                        "toPath": "u/text",
                        "toParentPath": "u",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [],
                        "fromPath": "u",
                        "fromParentPath": "u",
                        "from": "u",
                        "toPath": "u",
                        "toParentPath": "u",
                        "to": "u",
                    }, {
                        "attrs": [],
                        "fromPath": "u/text",
                        "fromParentPath": "u",
                        "from": "text",
                        "toPath": "u/text",
                        "toParentPath": "u",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: "u",
                    html: "u",
                    sub: {
                        akn: "text",
                        html: "u/text"
                    },
                }
            },

            aknHtmlBold: {
                normalizedConfig: {

                    "to": [{
                        "attrs": [],
                        "fromPath": "b",
                        "fromParentPath": "b",
                        "from": "b",
                        "toPath": "strong",
                        "toParentPath": "strong",
                        "to": "strong",
                    }, {
                        "attrs": [],
                        "fromPath": "b/text",
                        "fromParentPath": "b",
                        "from": "text",
                        "toPath": "strong/text",
                        "toParentPath": "strong",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [],
                        "fromPath": "strong",
                        "fromParentPath": "strong",
                        "from": "strong",
                        "toPath": "b",
                        "toParentPath": "b",
                        "to": "b",
                    }, {
                        "attrs": [],
                        "fromPath": "strong/text",
                        "fromParentPath": "strong",
                        "from": "text",
                        "toPath": "b/text",
                        "toParentPath": "b",
                        "to": "text"
                    }]

                },
                rawConfig: {
                    akn: "b",
                    html: "strong",
                    sub: {
                        akn: "text",
                        html: "strong/text"
                    },
                }
            },

            aknArticle: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [{
                            "from": "id",
                            "to": "id",
                            "action": "passAttributeTransformer"
                        }, {
                            "from": "editable",
                            "to": "contenteditable",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article",
                        "fromParentPath": "article",
                        "from": "article",
                        "toPath": "article",
                        "toParentPath": "article",
                        "to": "article"
                    }, {
                        "attrs": [{
                            "from": "editable",
                            "to": "contenteditable",
                            "toValue": "false",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article/num",
                        "fromParentPath": "article",
                        "from": "num",
                        "toPath": "article/h1",
                        "toParentPath": "article",
                        "to": "h1"
                    }, {
                        "attrs": [],
                        "fromPath": "article/num/text",
                        "fromParentPath": "article/num",
                        "from": "text",
                        "toPath": "article/h1/text",
                        "toParentPath": "article/h1",
                        "to": "text"
                    }, {
                        "attrs": [{
                            "from": "editable",
                            "to": "contenteditable",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article/heading",
                        "fromParentPath": "article",
                        "from": "heading",
                        "toPath": "article/h2",
                        "toParentPath": "article",
                        "to": "h2"
                    }, {
                        "attrs": [],
                        "fromPath": "article/heading/text",
                        "fromParentPath": "article/heading",
                        "from": "text",
                        "toPath": "article/h2/text",
                        "toParentPath": "article/h2",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [{
                            "to": "id",
                            "from": "id",
                            "action": "passAttributeTransformer"
                        }, {
                            "to": "editable",
                            "from": "contenteditable",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article",
                        "fromParentPath": "article",
                        "from": "article",
                        "toPath": "article",
                        "toParentPath": "article",
                        "to": "article"
                    }, {
                        "attrs": [{
                            "to": "editable",
                            "from": "contenteditable",
                            "fromValue": "false",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article/h1",
                        "fromParentPath": "article",
                        "from": "h1",
                        "toPath": "article/num",
                        "toParentPath": "article",
                        "to": "num"
                    }, {
                        "attrs": [],
                        "fromPath": "article/h1/text",
                        "fromParentPath": "article/h1",
                        "from": "text",
                        "toPath": "article/num/text",
                        "toParentPath": "article/num",
                        "to": "text"
                    }, {
                        "attrs": [{
                            "to": "editable",
                            "from": "contenteditable",
                            "action": "handleNonEditable"
                        }],
                        "fromPath": "article/h2",
                        "fromParentPath": "article",
                        "from": "h2",
                        "toPath": "article/heading",
                        "toParentPath": "article",
                        "to": "heading"
                    }, {
                        "attrs": [],
                        "fromPath": "article/h2/text",
                        "fromParentPath": "article/h2",
                        "from": "text",
                        "toPath": "article/heading/text",
                        "toParentPath": "article/heading",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: 'article',
                    html: 'article',
                    attr: [{
                        akn: "id",
                        html: "id"
                    }, {
                        akn: "editable",
                        html: "contenteditable"
                    }],
                    sub: [{
                        akn: "num",
                        html: "article/h1",
                        attr: [{
                            akn: "editable",
                            html: "contenteditable=false"
                        }],
                        sub: {
                            akn: "text",
                            html: "article/h1/text"
                        }
                    }, {
                        akn: "heading",
                        html: "article/h2",
                        attr: [{
                            akn: "editable",
                            html: "contenteditable"
                        }],
                        sub: {
                            akn: "text",
                            html: "article/h2/text"
                        }
                    }]
                }
            },

            aknOrderedList: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [],
                        "fromPath": "list",
                        "fromParentPath": "list",
                        "from": "list",
                        "toPath": "ol",
                        "toParentPath": "ol",
                        "to": "ol"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point",
                        "fromParentPath": "list",
                        "from": "point",
                        "toPath": "ol/li",
                        "toParentPath": "ol",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point/num",
                        "fromParentPath": "list/point",
                        "from": "num",
                        "toPath": "ol/li",
                        "toParentPath": "ol",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point/num/text",
                        "fromParentPath": "list/point/num",
                        "from": "text",
                        "toPath": "ol/li",
                        "toParentPath": "ol",
                        "to": "li",
                        "toAttribute": "num"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point/content",
                        "fromParentPath": "list/point",
                        "from": "content",
                        "toPath": "ol/li",
                        "toParentPath": "ol",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point/content/mp",
                        "fromParentPath": "list/point/content",
                        "from": "mp",
                        "toPath": "ol/li",
                        "toParentPath": "ol",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/point/content/mp/text",
                        "fromParentPath": "list/point/content/mp",
                        "from": "text",
                        "toPath": "ol/li/text",
                        "toParentPath": "ol/li",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [],
                        "fromPath": "ol",
                        "fromParentPath": "ol",
                        "from": "ol",
                        "toPath": "list",
                        "toParentPath": "list",
                        "to": "list"
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li",
                        "fromParentPath": "ol",
                        "from": "li",
                        "toPath": "list/point",
                        "toParentPath": "list",
                        "to": "point"
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li",
                        "fromParentPath": "ol",
                        "from": "li",
                        "toPath": "list/point/num",
                        "toParentPath": "list/point",
                        "to": "num",
                        "noNestedAllowed": true
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li",
                        "fromParentPath": "ol",
                        "from": "li",
                        "fromAttribute": "num",
                        "toPath": "list/point/num/text",
                        "toParentPath": "list/point/num",
                        "to": "text"
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li",
                        "fromParentPath": "ol",
                        "from": "li",
                        "toPath": "list/point/content",
                        "toParentPath": "list/point",
                        "to": "content"
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li",
                        "fromParentPath": "ol",
                        "from": "li",
                        "toPath": "list/point/content/mp",
                        "toParentPath": "list/point/content",
                        "to": "mp"
                    }, {
                        "attrs": [],
                        "fromPath": "ol/li/text",
                        "fromParentPath": "ol/li",
                        "from": "text",
                        "toPath": "list/point/content/mp/text",
                        "toParentPath": "list/point/content/mp",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: 'list',
                    html: 'ol',
                    sub: {
                        akn: 'point',
                        html: 'ol/li',
                        sub: [{
                            akn: 'num',
                            html: 'ol/li',
                            sub: {
                                akn: 'text',
                                html: 'ol/li[num]'
                            }
                        }, {
                            akn: 'content',
                            html: 'ol/li',
                            sub: {
                                akn: 'mp',
                                html: 'ol/li',
                                sub: {
                                    akn: 'text',
                                    html: 'ol/li/text'
                                }
                            }
                        }]
                    }
                }
            },

            aknUnorderedList: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [],
                        "fromPath": "list",
                        "fromParentPath": "list",
                        "from": "list",
                        "toPath": "ul",
                        "toParentPath": "ul",
                        "to": "ul"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent",
                        "fromParentPath": "list",
                        "from": "indent",
                        "toPath": "ul/li",
                        "toParentPath": "ul",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent/num",
                        "fromParentPath": "list/indent",
                        "from": "num",
                        "toPath": "ul/li",
                        "toParentPath": "ul",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent/num/text",
                        "fromParentPath": "list/indent/num",
                        "from": "text",
                        "toPath": "ul/li",
                        "toParentPath": "ul",
                        "to": "li",
                        "toAttribute": "num"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent/content",
                        "fromParentPath": "list/indent",
                        "from": "content",
                        "toPath": "ul/li",
                        "toParentPath": "ul",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent/content/mp",
                        "fromParentPath": "list/indent/content",
                        "from": "mp",
                        "toPath": "ul/li",
                        "toParentPath": "ul",
                        "to": "li"
                    }, {
                        "attrs": [],
                        "fromPath": "list/indent/content/mp/text",
                        "fromParentPath": "list/indent/content/mp",
                        "from": "text",
                        "toPath": "ul/li/text",
                        "toParentPath": "ul/li",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [],
                        "fromPath": "ul",
                        "fromParentPath": "ul",
                        "from": "ul",
                        "toPath": "list",
                        "toParentPath": "list",
                        "to": "list"
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li",
                        "fromParentPath": "ul",
                        "from": "li",
                        "toPath": "list/indent",
                        "toParentPath": "list",
                        "to": "indent"
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li",
                        "fromParentPath": "ul",
                        "from": "li",
                        "toPath": "list/indent/num",
                        "toParentPath": "list/indent",
                        "to": "num",
                        "noNestedAllowed": true
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li",
                        "fromParentPath": "ul",
                        "from": "li",
                        "fromAttribute": "num",
                        "toPath": "list/indent/num/text",
                        "toParentPath": "list/indent/num",
                        "to": "text"
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li",
                        "fromParentPath": "ul",
                        "from": "li",
                        "toPath": "list/indent/content",
                        "toParentPath": "list/indent",
                        "to": "content"
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li",
                        "fromParentPath": "ul",
                        "from": "li",
                        "toPath": "list/indent/content/mp",
                        "toParentPath": "list/indent/content",
                        "to": "mp"
                    }, {
                        "attrs": [],
                        "fromPath": "ul/li/text",
                        "fromParentPath": "ul/li",
                        "from": "text",
                        "toPath": "list/indent/content/mp/text",
                        "toParentPath": "list/indent/content/mp",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: 'list',
                    html: 'ul',
                    sub: {
                        akn: 'indent',
                        html: 'ul/li',
                        sub: [{
                            akn: 'num',
                            html: 'ul/li',
                            sub: {
                                akn: 'text',
                                html: 'ul/li[num]'
                            }
                        }, {
                            akn: 'content',
                            html: 'ul/li',
                            sub: {
                                akn: 'mp',
                                html: 'ul/li',
                                sub: {
                                    akn: 'text',
                                    html: 'ul/li/text'
                                }
                            }
                        }]
                    }
                }
            },

            aknAuthorialNote: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [{
                            "to": "class",
                            "toValue": "authorialnote",
                            "action": "addClassAttributeTransformer"
                        }, {
                            "from": "marker",
                            "to": "marker",
                            "action": "passAttributeTransformer"
                        }, {
                            "from": "id",
                            "to": "id",
                            "action": "passAttributeTransformer"
                        }],
                        "fromPath": "authorialnote",
                        "fromParentPath": "authorialnote",
                        "from": "authorialNote",
                        "toPath": "sup",
                        "toParentPath": "sup",
                        "to": "sup"
                    }, {
                        "attrs": [],
                        "fromPath": "authorialnote/mp",
                        "fromParentPath": "authorialnote",
                        "from": "mp",
                        "toPath": "sup",
                        "toParentPath": "sup",
                        "to": "sup"
                    }, {
                        "attrs": [],
                        "fromPath": "authorialnote/mp/text",
                        "fromParentPath": "authorialnote/mp",
                        "from": "text",
                        "toPath": "sup",
                        "toParentPath": "sup",
                        "to": "sup",
                        "toAttribute": "title"
                    }],
                    "from": [{
                        "attrs": [{
                            "from": "class",
                            "fromValue": "authorialnote"
                        }, {
                            "to": "marker",
                            "from": "marker",
                            "action": "passAttributeTransformer"
                        }, {
                            "to": "id",
                            "from": "id",
                            "action": "passAttributeTransformer"
                        }],
                        "fromPath": "sup",
                        "fromParentPath": "sup",
                        "from": "sup",
                        "toPath": "authorialnote",
                        "toParentPath": "authorialnote",
                        "to": "authorialNote"
                    }, {
                        "attrs": [],
                        "fromPath": "sup",
                        "fromParentPath": "sup",
                        "from": "sup",
                        "toPath": "authorialnote/mp",
                        "toParentPath": "authorialnote",
                        "to": "mp",
                        "noNestedAllowed": true
                    }, {
                        "attrs": [],
                        "fromPath": "sup",
                        "fromParentPath": "sup",
                        "from": "sup",
                        "fromAttribute": "title",
                        "toPath": "authorialnote/mp/text",
                        "toParentPath": "authorialnote/mp",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: 'authorialNote',
                    html: 'sup',
                    attr: [{
                        html: "class=authorialnote"
                    }, {
                        akn: "marker",
                        html: "marker"
                    }, {
                        akn: "id",
                        html: "id"
                    }],
                    sub: {
                        akn: 'mp',
                        html: 'sup',
                        sub: [{
                            akn: 'text',
                            html: 'sup[title]'
                        }]
                    }
                }
            }

            ,

            aknHtmlAnchor: {
                normalizedConfig: {
                    "to": [{
                        "attrs": [{
                            "from": "href",
                            "to": "href",
                            "action": "passAttributeTransformer"
                        }],
                        "fromPath": "a",
                        "fromParentPath": "a",
                        "from": "a",
                        "toPath": "a",
                        "toParentPath": "a",
                        "to": "a"
                    }, {
                        "attrs": [],
                        "fromPath": "a/text",
                        "fromParentPath": "a",
                        "from": "text",
                        "toPath": "a/text",
                        "toParentPath": "a",
                        "to": "text"
                    }],
                    "from": [{
                        "attrs": [{
                            "to": "href",
                            "from": "href",
                            "action": "passAttributeTransformer"
                        }],
                        "fromPath": "a",
                        "fromParentPath": "a",
                        "from": "a",
                        "toPath": "a",
                        "toParentPath": "a",
                        "to": "a"
                    }, {
                        "attrs": [],
                        "fromPath": "a/text",
                        "fromParentPath": "a",
                        "from": "text",
                        "toPath": "a/text",
                        "toParentPath": "a",
                        "to": "text"
                    }]
                },
                rawConfig: {
                    akn: 'a',
                    html: 'a',
                    attr: [{
                        akn: "href",
                        html: "href"
                    }],
                    sub: {
                        akn: "text",
                        html: "a/text"
                    }
                }
            }
        };
    }).methods({

        getNormalizedConfig: function getNormalizedConfig(rawConfig) {
            var configNormalizer = ConfigNormalizerStamp();
            var normalizedConfig = configNormalizer.getNormalizedConfig({
                rawConfig: rawConfig
            });
            return normalizedConfig;
        },

        checkIfInSync: function checkIfInSync() {
            var that = this;
            LODASH.forEach(this.configs, function(value, key) {
                if (!value.normalizedConfig || !value.rawConfig) {
                    var errorMsg = ["Following params is required: ", "normalizedConfig", ",", "rawConfig"].join("");
                    LOG.error(errorMsg);
                    throw errorMsg;

                }
                var normalizedConfigFromRawConfig = that.getNormalizedConfig(value.rawConfig);
                var normalizedConfigFromRawConfigAsString = JSON.stringify(normalizedConfigFromRawConfig);
                var normalizedConfigAsString = JSON.stringify(value.normalizedConfig);
                if (normalizedConfigFromRawConfigAsString !== normalizedConfigAsString) {
                    var errorMsg = ["Seems that for transformation: ", key, ", normalized config is out of sync with raw config."].join("");
                    LOG.error(errorMsg);
                }
            });
        }
    });

    var transformationsConfigUtil = transformationsConfigUtilStamp();
    transformationsConfigUtil.checkIfInSync();

    return transformationsConfigUtil;
});