URL_REGEX="//(\w+\.)(\w+\.?)+"

grep -oE $URL_REGEX "$1" |sort |uniq | sed -e "s/\/\///"
