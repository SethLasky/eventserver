# Event Server
This is a server that is used to write events and report frequency of events. To run the service follow the steps in Quick Start.

## Quick Start
Run the following commands:
1. ```sbt docker:publishLocal```

2. ```docker-compose up -d```

3. Write an event using the following command:
   ```curl --location --request POST 'http://localhost:8080/analytics?timestamp={millis_since_epoch}&user={user_id}&event={click|impression}```

4. Get all unique users, clicks, and impressions for the hour of the timestamp specified with the following command:
   ```curl --location --request GET 'http://localhost:8080/analytics?timestamp={millis_since_epoch}```

### Run application
`sbt run`

### Run tests    
    docker-compose -f docker-compose-test.yml up -d
    sbt test
    

### Build docker image locally
`sbt docker:publishLocal`

### Create Dockerfile 
`sbt docker:stage` 
