## 0.2.* => 1.0.0
- from `upsert-foreign! [conn [id-ks id] parent-ks parent-id child-ks to-be-saved]`
to  
`upsert-foreign! [conn parent-ks parent-id child-ks to-be-saved]`

  - id-ks and id where redundant with parent-ks and parent-id,   
so they were unified

## 1.* => 2.0.0
- `upsert-foreign! [conn parent-ks parent-id child-ks to-be-saved]`
  to  
  `upsert-foreign! [conn foreign-ks foreign-id ref-ks main-ks main-id]`

  - upsert foreign was not working at all, now it is inserting or updating foreign refs with :db/add
