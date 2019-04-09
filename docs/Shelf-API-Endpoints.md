# Library API Endpoints

## Get:

#### /
- Returns map of all versions of all knowledge objects as flattened json-ld
 - 200 ok on success

#### /{naan}/{name}
- Returns a map of all versions of a single knowledge object as flattened json-ld
- 200 ok if success

#### /{naan}/{name}/{implementation}
- returns a implementation of an object as flattened json-ld
- 200 ok if success

#### /{naan}/{name}/{implementation}/{path}
- Get the metadata.json file for that path
- Returns metadata as flattened json-ld
- 200 ok on success

#### /{naan}/{name}/{implementation} 
##### with header accept = application/zip
- returns a zipped copy of a complete implementation of the knowledge object
- 200 ok if success

## Post:

#### / 
- Deposits a zipped knowledge object onto the shelf and detects the ark id from the folder name in the zip
- multipart data needs to have a complete zipped knowledge object with key "ko"
- 201 created on success

#### /
- fetches zipped ko(s) and puts them on the shelf
- requires a json body like {"ko":"url here"} or {"ko":["url1", "url2", ...]}
- 201 created on success 

## Put:

#### /{naan}/{name}/{implementation}
- creates a new knowledge object from a multipart file upload
- multipart data needs to have a complete zipped knowledge object with key "ko"
- 201 created on success

#### /{naan}/{name}/{implementation}
- Replace the metadata at the root of the object with the body of the request in json-ld
- Returns the new metadata as flattened json-ld
- 200 ok on success

#### /{naan}/{name}/{implementation}/{path}
- Replace the metadata at the specified path in the object with the body of the request in json-ld
- Returns the new metadata as flattened json-ld
- 200 ok on success

## Delete:

#### /{naan}/{name}
- Deletes the specified knowledge object
- returns a 204 no content on success 

#### /{naan}/{name}/{implementation}
- Deletes the specified knowledge object
- returns a 204 no content on success 


\*It is possible to use {naan}-{name} in any url in place of {naan}/{name}