import angular from 'angular';

type Lojector = {
  get: <T>(name: string) => T;
};

let lojector: Lojector;

let $q: any; // ng promise

const initLo = ($injector: Lojector, q) => {
  lojector = $injector;
  $q = q;
};

export { initLo, lojector, $q };
