import React, { useEffect, useState } from "react";

function Greeting({ name }: { name: string }) {
  return <h2>Hello, {name}!</h2>;
}

function Counter() {
  const [count, setCount] = useState(0);
  useEffect(() => {
    document.title = `Clicked ${count} times`;
  }, [count]);
  return (
    <div>
      <p>Clicked {count} times</p>
      <button onClick={() => setCount((c) => c + 1)}>Click me</button>
    </div>
  );
}

class ClassMessage extends React.Component<{ text: string }> {
  render() {
    return <p>This is a class component: {this.props.text}</p>;
  }
}

export default function App() {
  return (
    <div>
      <h1>Hello World in React 🎉</h1>
      <Greeting name="Alice" />
      <Counter />
      <ClassMessage text="React also supports classes!" />
    </div>
  );
}
