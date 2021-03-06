/* Globals */
/* Graph style */
const graphStyle = [
  {
    "selector": "node",
    "style": {
      "background-color": "white",
      "border-color": "#fff",
      "border-width": 0,
      "border-style": "solid",
      "width": 20,
      "height": 20,
      "min-zoomed-font-size": 12,
      "color": "#fff",
      "font-size": 16,
      "z-index": 2
    }
  },

  {
    "selector": ".mine",
    "style": {
      "background-color": "red",
      "border-color": "white",
      "width": 120,
      "height": 120,
      "color": "#000",
      "font-size": 72,
      "font-weight": "bold",
      "z-index": 9999,
      "text-valign": "center",
      "text-halign": "center",
      "content": "λ"
      }
  },

  {
    selector: '.mine.highlighted',
    style: {
      "width": 240,
      "height": 240,
      "font-size": 144,
      "background-color": '#ff0000',
    }
  },

  {
    selector: '.plain.highlighted',
    style: {
      "width": 240,
      "height": 240,
      "background-color": '#ffffff',
    }
  },

  { "selector": ".plain:selected",
    "style": {
    "height": 40,
    "width": 40,
    "content": "data(id)",
    "font-size": 140,
    "border-color": "#000",
    "border-width": "1px",
    "text-outline-color": "#000",
    "text-outline-width": "1px",
    "z-index": 9999
    }
  },

  { "selector": "edge",
    "style": {
      "min-zoomed-font-size": 1,
      "font-size": 64,
      "color": "#FFF",
      "line-color": "#00F",
      "width": 10,
      "z-index" : 1,
      "curve-style": "haystack",
      "haystack-radius": 0
      }
  },

  {
    selector: 'edge.highlighted',
    style: {
      width: 30,
      'z-index': 4,
    }
  },

  {
    selector: 'edge[option]',
    style: {
      width: 30,
      'z-index': 0,
    }
  },

  { "selector": "edge:selected",
    "style": {
      "min-zoomed-font-size": 1,
      "font-size": 4,
      "color": "#fff",
      "line-color": "yellow",
      "width": 10
      }
  },


  {"selector": "core",
    "style": {
    "active-bg-color": "#fff",
    "active-bg-opacity": 0.333
    }
  },

  {
    "selector": ".option",
    "style": {
      "width": 30,
    },
  }
];

// Set of nodes which are mines
const mineSet = new Set();

// ID Sources
let highestNodeID = 0;
let highestEdgeID = 0;

// Key state
let shiftPressed = false;
let altPressed = false;

function getFreshNodeID() {
  highestNodeID++;
  return highestNodeID;
}

function getFreshEdgeID() {
  highestEdgeID++;
  return "e" + highestEdgeID;
}

function initCy( expJson, after ){
  mineSet.clear();
  const loading = document.getElementById('loading');
  const elements = importJSON(expJson);

  loading.classList.add('loaded');

  const cy = window.cy = cytoscape({
    container: document.getElementById('cy'),
    layout: { name: 'preset' },
    style: graphStyle,
    elements: elements,
    motionBlur: true,
    selectionType: 'single',
    boxSelectionEnabled: false
  });

  cy.ready(function(evt) { activateMines(); after() });
}

function loadGraph(graphJSON, activateBindings) {
  //cy.remove("");
  const toImport = importJSON(graphJSON);
  cy.add(toImport);
  activateMines();
  activateBindings();
  cy.resize();
}

function normRiver(r) {
  const source = r.source;
  const target = r.target;
  if (source > target) {
    r.source = target;
    r.target = source;
  }
}

/* JSON IMPORT */

// Our JSON -> Cyto JSON
function importJSON(ourJSON) {
  highestNodeID = 0;
  highestEdgeID = 0;

  if (ourJSON.sites == undefined || ourJSON.rivers == undefined) {
    return;
  }

  const scale = findScaleFactor(ourJSON) * (Math.log(ourJSON.sites.length) / 1.5 );
  //console.log("Scale factor: " + scale);
  const elements = [];

  // Firstly, mark all "mine" nodes as mines within their data
  if ("mines" in ourJSON && ourJSON.mines != null) {
    for (let i = 0; i < ourJSON.mines.length; i++) {
      const mineID = ourJSON.mines[i];
      //console.log("Adding " + mineID + " to mineSet");
      mineSet.add(mineID.toString());
    }
  }

  // Add all nodes
  for (let i = 0; i < ourJSON.sites.length; i++) {
    const curNode = ourJSON.sites[i];
    if (curNode.id > highestNodeID) {
      highestNodeID = curNode.id;
    }

    const entry = {group: "nodes"};

    if (!mineSet.has(curNode.id.toString())) {
      entry["classes"] = "plain";
    }

    entry["data"] = {
      "id": curNode.id.toString(),
      "x": curNode.x * scale,
      "y" : curNode.y * scale,
      "selected": false,
    };

    const position = { "x": curNode.x * scale, "y": curNode.y * scale};
    entry["position"] = position;
    elements.push(entry);
  }

  for (let i = 0; i < ourJSON.rivers.length; i++) {
    const id = getFreshEdgeID();
    const curRiver = ourJSON.rivers[i];
    normRiver(curRiver);
    const entry = {group: "edges"};
    const data = { "id": id, "source": curRiver["source"].toString(), "target": curRiver["target"].toString()};
    entry["data"] = data;
    entry["selected"] = false;
    entry["classes"] = "top-center";
    elements.push(entry);
  }
  return elements;
}

/* DISPLAY FUNCTIONS */
function findScaleFactor(ourJSON) {
  let maxX = -Infinity;
  let maxY = -Infinity;
  let minX = Infinity;
  let minY = Infinity;

  for (let i = 0; i < ourJSON.sites.length; i++) {
    const node = ourJSON.sites[i];
    if (Math.abs(node["x"]) > maxX) {
      maxX = node["x"];
    }
    if (Math.abs(node["x"]) < minX) {
      minX = node["x"];
    }
    if (Math.abs(node["y"]) > maxY) {
      maxY = node["y"];
    }
    if (Math.abs(node["y"]) < minY) {
      minY = node["y"];
    }
  }
  //console.log("maxX: " + maxX);
  //console.log("maxY: " + maxY);

  const scaleX = window.innerWidth / (maxX - minX);
  const scaleY = window.innerHeight / (maxY - minY);
  return Math.min(Math.abs(scaleX), Math.abs(scaleY));
}

function activateMines() {
  for (let mineID of mineSet) {
    cy.$id(mineID).addClass("mine");
  }
}

