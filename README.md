# Warpol API

This is a recruitment assignment for Warpo.

You can find a ready-to-import Postman collection of all endpoints in the [commands.postman_collection.json](https://github.com/Wojtur28/warpol_game/blob/main/commands.postman_collection.json) file in this repo.


Demo: http://194.163.166.24:8081/swagger-ui/index.html (I hope it's work)

## API Reference

### Create new game

```
  POST /api/v1/game/new
```

---

### Get units by Color

```
  GET /api/v1/units?color=BLACK
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `color` | `string` | `Required. Player color BLACK/WHITE` |

---

### Execute command

```
  POST /api/v1/command/execute
```

#### Example request body

    {
      "playerColor": "BLACK",
      "command": {
      "unitId": "938ac58e-9982-4557-808a-f8be1776bb4e",
      "commandType": "MOVE",
      "targetX": 7,
      "targetY": 6
      }
    }

---

### Execute random command

```
  POST /api/v1/command/random
```

#### Example request body

    {
      "playerColor": "BLACK",
      "command": "3b58dcc3-8091-4149-9d22-80ad5788d638"
    }



    
