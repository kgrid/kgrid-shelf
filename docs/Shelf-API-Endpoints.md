# Library API Endpoints

## Get:

#### /
- Returns map of all versions of all knowledge objects as flattened json-ld
 - 200 ok on success

#### /{naan}/{name}
- Returns a map of all versions of a single knowledge object as flattened json-ld
- 200 ok if success

#### /{naan}/{name}/{version}
- returns a version of an object as flattened json-ld
- 200 ok if success

#### /{naan}/{name}/{version}/{path}
- Get the metadata.json file for that path
- Returns metadata as flattened json-ld
- 200 ok on success

#### /{naan}/{name}/{version} 
##### with header accept = application/zip
- returns a zipped copy of a complete version of the knowledge object
- 200 ok if success

## Put:

#### /{naan}/{name}/{version}
- creates a new knowledge object from a multipart file upload
- multipart data needs to have a complete zipped knowledge object with key "ko"
- 201 created on success

#### /{naan}/{name}/{version}
- Replace the metadata at the root of the object with the body of the request in json-ld
- Returns the new metadata as flattened json-ld
- 200 ok on success

#### /{naan}/{name}/{version}/{path}
- Replace the metadata at the specified path in the object with the body of the request in json-ld
- Returns the new metadata as flattened json-ld
- 200 ok on success

## Delete:

#### /{naan}/{name}
- Deletes the specified knowledge object
- returns a 204 no content on success 

#### /{naan}/{name}/{version}
- Deletes the specified knowledge object
- returns a 204 no content on success 


\*It is possible to use {naan}-{name} in any url in place of {naan}/{name}