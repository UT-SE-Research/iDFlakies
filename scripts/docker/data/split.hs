#!/usr/bin/env stack
{- stack
   script
   --resolver lts-12.12
   --package MissingH
   --package http-types
   --package bytestring
   --package text
   --package directory
-}

import qualified Data.ByteString.Char8 as Char8
import Data.Char
import Data.List
import Data.String.Utils
import qualified Data.Text as T

import Network.HTTP.Types

import System.Directory
import System.Environment

name = map toLower . intercalate "." . map T.unpack . drop 3 . decodePathSegments . Char8.pack

write prefix l = do
    let n = name $ head $ split "," l

    writeFile (prefix ++ "/" ++ n ++ ".csv") l

main = do
    createDirectoryIfMissing True "individual-split"
    mapM_ (write "individual-split") =<< lines <$> (readFile =<< head <$> getArgs)

