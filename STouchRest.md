### The STouchREST API

The `STouchREST` API emulates the S-Touch app to interact with the Paradigma SystaComfort system. It provides endpoints for connecting to the device, simulating touch events, retrieving the screen state, and automating sequences of actions.

#### connect
`POST` `/stouchrest/connect`  
Establishes a connection to the SystaComfort unit.
```bash
curl -X POST http://systapi:1337/stouchrest/connect
```
Responses:  
`200 OK`: Connection successful.  
`409 Conflict`: Device is already in use or already connected.  
`401 Unauthorized`: Wrong UDP password.  
`408 Request Timeout`: Connection request timed out.  
`500 Internal Server Error`: Unknown error.

#### disconnect
`POST` `/stouchrest/disconnect`  
Disconnects from the SystaComfort unit.
```bash
curl -X POST http://systapi:1337/stouchrest/disconnect
```
Responses:  
`200 OK`: Disconnection successful.  
`500 Internal Server Error`: Error occurred during disconnection.

#### touch
`POST` `/stouchrest/touch`  
Simulates a touch event at the specified coordinates on the screen.

Query Parameters:  
`x` (int): The x-coordinate of the touch.  
`y` (int): The y-coordinate of the touch.
```bash
curl -X POST "http://systapi:1337/stouchrest/touch?x=100&y=200"
```
Responses:  
`200 OK`: Touch event simulated successfully.

#### screen
`GET` `/stouchrest/screen`  
[http://systapi:1337/STouchREST/screen](http://systapi:1337/STouchREST/screen)  
Retrieves the current screen as a PNG image.
```bash
curl -X GET http://systapi:1337/stouchrest/screen --output screen.png
```
Responses:  
`200 OK`: Returns the screen image as a PNG.  
`500 Internal Server Error`: Error processing the image.

#### debugscreen
`GET` `/stouchrest/debugscreen`  
[http://systapi:1337/STouchREST/debugscreen](http://systapi:1337/STouchREST/debugscreen)  
Returns an interactive HTML page for debugging touch events on the screen. The HTML shows the current screen and a history of the touch events.
```bash
curl -X GET http://systapi:1337/stouchrest/debugscreen
```
Responses:  
`200 OK`: Returns the HTML page for debugging.  
`500 Internal Server Error`: Error processing the image.

#### objecttree
`GET` `/stouchrest/objecttree`  
[http://systapi:1337/STouchREST/objecttree](http://systapi:1337/STouchREST/objecttree)  
Returns the object tree of the current screen as a JSON object.
```bash
curl -X GET http://systapi:1337/stouchrest/objecttree
```
Responses:  
`200 OK`: Returns the object tree as JSON.

#### touchbutton
`POST` `/stouchrest/touchbutton`  
Simulates a touch event on a button with the specified ID.
```bash
curl -X POST "http://systapi:1337/stouchrest/touchbutton?id=1"
```
Query Parameters:  
`id` (byte): The ID of the button to touch.
Responses:

`200 OK`: Button pressed successfully.  
`404 Not Found`: Button with the specified ID not found.  
`500 Internal Server Error`: Error while pressing the button.

#### touchtext
`POST` `/stouchrest/touchtext`  
Simulates a touch event on the specified text.
```bash
curl -X POST "http://systapi:1337/stouchrest/touchtext?text=Hello"
```
Query Parameters:  
`text` (String): The text to touch.
Responses:

`200 OK`: Text touched successfully.  
`404 Not Found`: Text not found on the display.  
`500 Internal Server Error`: Error while touching the text.

#### automation
`GET` `/stouchrest/automation`  
[http://systapi:1337/STouchREST/automation](http://systapi:1337/STouchREST/automation)  
Executes a sequence of commands provided as query parameters. Between each command a delay of two seconds is added, because the reply sometimes need a while. To prevent communication problems, you should always start an automation with `connect` and finish it with `disconnect`.

Query Parameters:  
Query parameters is a list of supported commands, seperated by &:  
`connect`: Connect to the SystaComfort unit.  
`touch=x,y`: Simulate a touch event on screen coordinates `x` and `y` as integers.  
`touchText=text`: Simulate a touch event on the given text.  
`touchButton=id`: Simulate a touch event on the button with the given ID.  
`whileText==text&doAction`: While the given text is in the object tree, do the given action. `text` needs to be URL encoded, e.g. blank is %20. `doAction` is any supported command  
`whileText!=text&doAction`: While the given text is not in the object tree, do the given action. `text` needs to be URL encoded, e.g. blank is %20. `doAction` is any supported command  
`whileButton==id&doAction`: While the button with the given ID is in the object tree, do the given `action`. `id` is any integer,`doAction` is any supported command  
`whileButton!=id&doAction`: While the button with the given ID is not in the object tree, do the given `action`. `id` is any integer,`doAction` is any supported command  
`checkText==text&thenDoThisAction&elseDoThisAction`: If the given text is in the object tree, then excute the command given by `thenDoThisAction`, otherwise execute the command given in `elseDoThisAction`.  
`checkText!=text&thenDoThisAction&elseDoThisAction`: If the given text is not in the object tree, then excute the command given by `thenDoThisAction`, otherwise execute the command given in `elseDoThisAction`.  
`checkButton==id&thenDoThisAction&elseDoThisAction`: If the button with the given `id` is in the object tree, then execute the command given by `thenDoThisAction`, otherwise execute the command given in `elseDoThisAction`.  
`checkButton!=id&thenDoThisAction&elseDoThisAction`: If the button with the given `id` is not in the object tree, then execute the command given by `thenDoThisAction`, otherwise execute the command given in `elseDoThisAction`.  
`disconnect`: Disconnect from the SystaComfort unit.  
```bash
# switch to Heating program 1
curl -X GET "http://systapi:1337/stouchrest/automation?connect&touch=155,141&touch=42,77&touch=157,182&whiletext!=Heating%20program%201&touchbutton=19&touchtext=Heating%20program%201&disconnect"
# switch to Off
curl -X GET "http://systapi:1337/stouchrest/automation?connect&touch=155,141&touch=42,77&touch=157,182&whiletext!=Off&touchbutton=20&touchtext=Off&disconnect"
```
Responses:

`200 OK`: Automation executed successfully.  
`500 Internal Server Error`: Command failed.
