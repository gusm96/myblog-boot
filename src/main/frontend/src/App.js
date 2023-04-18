import React, {useEffect, useState} from "react";
import axios from "axios";
export default App

function App(){
  const [hello, setHello] = useState('');

  useEffect(() => {
    axios.get('/api/hello')
        .then(response => setHello(response.data))
        .catch(error => console.log(error))
  }, []);

  return (
      <div>
        안녕하세요 : {hello}
      </div>
  )
}