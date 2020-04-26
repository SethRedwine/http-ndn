var express = require("express");
var bodyParser = require('body-parser');
var cors = require('cors')
var path = require('path');
var app = express();
app.use(bodyParser.json()); // support json encoded bodies
app.use(bodyParser.urlencoded({ extended: true })); // support encoded bodies
app.use(cors());

app.get('/index.html', (req, res, next) => {
    res.sendFile(path.join(__dirname + '/index.html'));
});

app.get("/test-get", (req, res, next) => {
    console.log("GET /test-get");
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json("<!DOCTYPE html><body><span>HELLO</span></body>");
    next();
});

app.post("/test-post", (req, res, next) => {
    console.log("POST /test-post");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.put("/test-put", (req, res, next) => {
    console.log("PUT /test-put");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.head("/test-head", (req, res, next) => {
    console.log("HEAD /test-head");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.options("/test-options", (req, res, next) => {
    console.log("OPTIONS /test-options");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.delete("/test-delete", (req, res, next) => {
    console.log("DELETE /test-delete");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.trace("/test-trace", (req, res, next) => {
    console.log("TRACE /test-trace");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.connect("/test-connect", (req, res, next) => {
    console.log("CONNECT /test-connect");
    console.log("request: ", req.body);
    res.header("Access-Control-Allow-Headers", "*");
    res.header("Access-Control-Allow-Methods", "*");
    res.header("Cache-Control", "no-cache");
    res.json(`<!DOCTYPE html><body><span>${JSON.stringify(req.body)}</span></body>`);
    next();
});

app.listen(3000, () => {
    console.log("Server running on port 3000");
});